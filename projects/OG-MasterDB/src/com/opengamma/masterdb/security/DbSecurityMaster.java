/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.masterdb.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hsqldb.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.LobHandler;

import com.google.common.collect.Lists;
import com.opengamma.extsql.ExtSqlBundle;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.ExternalIdSearch;
import com.opengamma.id.ObjectId;
import com.opengamma.id.ObjectIdentifiable;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.security.ManageableSecurity;
import com.opengamma.master.security.RawSecurity;
import com.opengamma.master.security.SecurityDocument;
import com.opengamma.master.security.SecurityHistoryRequest;
import com.opengamma.master.security.SecurityHistoryResult;
import com.opengamma.master.security.SecurityMaster;
import com.opengamma.master.security.SecurityMetaDataRequest;
import com.opengamma.master.security.SecurityMetaDataResult;
import com.opengamma.master.security.SecuritySearchRequest;
import com.opengamma.master.security.SecuritySearchResult;
import com.opengamma.master.security.SecuritySearchSortOrder;
import com.opengamma.masterdb.AbstractDocumentDbMaster;
import com.opengamma.masterdb.security.hibernate.HibernateSecurityMasterDetailProvider;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.db.DbConnector;
import com.opengamma.util.db.DbDateUtils;
import com.opengamma.util.db.DbMapSqlParameterSource;
import com.opengamma.util.paging.Paging;

/**
 * A security master implementation using a database for persistence.
 * <p>
 * This is a full implementation of the security master using an SQL database.
 * Full details of the API are in {@link SecurityMaster}.
 * <p>
 * The SQL is stored externally in {@code DbSecurityMaster.extsql}.
 * Alternate databases or specific SQL requirements can be handled using database
 * specific overrides, such as {@code DbSecurityMaster-MySpecialDB.extsql}.
 * <p>
 * This class is mutable but must be treated as immutable after configuration.
 */
public class DbSecurityMaster extends AbstractDocumentDbMaster<SecurityDocument> implements SecurityMaster {

  /** Logger. */
  private static final Logger s_logger = LoggerFactory.getLogger(DbSecurityMaster.class);

  /**
   * The default scheme for unique identifiers.
   */
  public static final String IDENTIFIER_SCHEME_DEFAULT = "DbSec";

  /**
   * SQL order by.
   */
  protected static final EnumMap<SecuritySearchSortOrder, String> ORDER_BY_MAP = new EnumMap<SecuritySearchSortOrder, String>(SecuritySearchSortOrder.class);
  static {
    ORDER_BY_MAP.put(SecuritySearchSortOrder.OBJECT_ID_ASC, "oid ASC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.OBJECT_ID_DESC, "oid DESC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.VERSION_FROM_INSTANT_ASC, "ver_from_instant ASC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.VERSION_FROM_INSTANT_DESC, "ver_from_instant DESC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.NAME_ASC, "name ASC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.NAME_DESC, "name DESC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.SECURITY_TYPE_ASC, "sec_type ASC");
    ORDER_BY_MAP.put(SecuritySearchSortOrder.SECURITY_TYPE_DESC, "sec_type DESC");
  }

  /**
   * The detail provider.
   */
  private SecurityMasterDetailProvider _detailProvider;

  /**
   * Creates an instance.
   * 
   * @param dbConnector  the database connector, not null
   */
  public DbSecurityMaster(final DbConnector dbConnector) {
    super(dbConnector, IDENTIFIER_SCHEME_DEFAULT);
    setExtSqlBundle(ExtSqlBundle.of(dbConnector.getDialect().getExtSqlConfig(), DbSecurityMaster.class));
    setDetailProvider(new HibernateSecurityMasterDetailProvider());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the detail provider.
   * 
   * @return the detail provider
   */
  public SecurityMasterDetailProvider getDetailProvider() {
    return _detailProvider;
  }

  /**
   * Sets the detail provider.
   * 
   * @param detailProvider  the detail provider, not null
   */
  public void setDetailProvider(SecurityMasterDetailProvider detailProvider) {
    ArgumentChecker.notNull(detailProvider, "detailProvider");
    detailProvider.init(this);
    _detailProvider = detailProvider;
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityMetaDataResult metaData(SecurityMetaDataRequest request) {
    ArgumentChecker.notNull(request, "request");
    SecurityMetaDataResult result = new SecurityMetaDataResult();
    if (request.isSecurityTypes()) {
      final String sql = getExtSqlBundle().getSql("SelectTypes");
      List<String> securityTypes = getJdbcTemplate().getJdbcOperations().queryForList(sql, String.class);
      result.getSecurityTypes().addAll(securityTypes);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public SecuritySearchResult search(final SecuritySearchRequest request) {
    ArgumentChecker.notNull(request, "request");
    ArgumentChecker.notNull(request.getPagingRequest(), "request.pagingRequest");
    ArgumentChecker.notNull(request.getVersionCorrection(), "request.versionCorrection");
    s_logger.debug("search {}", request);
    
    final SecuritySearchResult result = new SecuritySearchResult();
    final ExternalIdSearch externalIdSearch = request.getExternalIdSearch();
    final List<ObjectId> objectIds = request.getObjectIds();
    if ((objectIds != null && objectIds.size() == 0) ||
        (ExternalIdSearch.canMatch(request.getExternalIdSearch()) == false)) {
      result.setPaging(Paging.of(request.getPagingRequest(), 0));
      return result;
    }
    final VersionCorrection vc = request.getVersionCorrection().withLatestFixed(now());
    final DbMapSqlParameterSource args = new DbMapSqlParameterSource()
      .addTimestamp("version_as_of_instant", vc.getVersionAsOf())
      .addTimestamp("corrected_to_instant", vc.getCorrectedTo())
      .addValueNullIgnored("name", getDialect().sqlWildcardAdjustValue(request.getName()))
      .addValueNullIgnored("sec_type", request.getSecurityType())
      .addValueNullIgnored("external_id_value", getDialect().sqlWildcardAdjustValue(request.getExternalIdValue()));
    if (externalIdSearch != null && externalIdSearch.alwaysMatches() == false) {
      int i = 0;
      for (ExternalId id : externalIdSearch) {
        args.addValue("key_scheme" + i, id.getScheme().getName());
        args.addValue("key_value" + i, id.getValue());
        i++;
      }
      args.addValue("sql_search_external_ids_type", externalIdSearch.getSearchType());
      args.addValue("sql_search_external_ids", sqlSelectIdKeys(externalIdSearch));
      args.addValue("id_search_size", externalIdSearch.getExternalIds().size());
    }
    if (objectIds != null) {
      StringBuilder buf = new StringBuilder(objectIds.size() * 10);
      for (ObjectId objectId : objectIds) {
        checkScheme(objectId);
        buf.append(extractOid(objectId)).append(", ");
      }
      buf.setLength(buf.length() - 2);
      args.addValue("sql_search_object_ids", buf.toString());
    }
    args.addValue("sort_order", ORDER_BY_MAP.get(request.getSortOrder()));
    args.addValue("paging_offset", request.getPagingRequest().getFirstItem());
    args.addValue("paging_fetch", request.getPagingRequest().getPagingSize());
    
    final SecurityMasterDetailProvider detailProvider = getDetailProvider();  // lock against change
    if (detailProvider != null) {
      detailProvider.extendSearch(request, args);
    }
    
    String[] sql = {getExtSqlBundle().getSql("Search", args), getExtSqlBundle().getSql("SearchCount", args)};
    searchWithPaging(request.getPagingRequest(), sql, args, new SecurityDocumentExtractor(), result);
    if (request.isFullDetail()) {
      loadDetail(detailProvider, result.getDocuments());
    }
    return result;
  }

  /**
   * Gets the SQL to find all the ids for a single bundle.
   * <p>
   * This is too complex for the extsql mechanism.
   * 
   * @param idSearch  the identifier search, not null
   * @return the SQL, not null
   */
  protected String sqlSelectIdKeys(final ExternalIdSearch idSearch) {
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < idSearch.size(); i++) {
      list.add("(key_scheme = :key_scheme" + i + " AND key_value = :key_value" + i + ") ");
    }
    return StringUtils.join(list, "OR ");
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityDocument get(final UniqueId uniqueId) {
    final SecurityDocument doc = doGet(uniqueId, new SecurityDocumentExtractor(), "Security");
    loadDetail(getDetailProvider(), Collections.singletonList(doc));
    return doc;
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityDocument get(final ObjectIdentifiable objectId, final VersionCorrection versionCorrection) {
    final SecurityDocument doc = doGetByOidInstants(objectId, versionCorrection, new SecurityDocumentExtractor(), "Holiday");
    loadDetail(getDetailProvider(), Collections.singletonList(doc));
    return doc;
  }

  //-------------------------------------------------------------------------
  @Override
  public SecurityHistoryResult history(final SecurityHistoryRequest request) {
    final SecurityHistoryResult result = doHistory(request, new SecurityHistoryResult(), new SecurityDocumentExtractor());
    if (request.isFullDetail()) {
      loadDetail(getDetailProvider(), result.getDocuments());
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the detail of the security for the document.
   * 
   * @param detailProvider  the detail provider, null ignored
   * @param docs  the documents to load detail for, not null
   */
  protected void loadDetail(final SecurityMasterDetailProvider detailProvider, final List<SecurityDocument> docs) {
    if (detailProvider != null) {
      for (SecurityDocument doc : docs) {
        if (!(doc.getSecurity() instanceof RawSecurity)) {
          doc.setSecurity(detailProvider.loadSecurityDetail(doc.getSecurity()));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Inserts a new document.
   * 
   * @param document  the document, not null
   * @return the new document, not null
   */
  @Override
  protected SecurityDocument insert(final SecurityDocument document) {
    ArgumentChecker.notNull(document.getSecurity(), "document.security");
    
    final long docId = nextId("sec_security_seq");
    final long docOid = (document.getUniqueId() != null ? extractOid(document.getUniqueId()) : docId);
    // the arguments for inserting into the security table
    final DbMapSqlParameterSource docArgs = new DbMapSqlParameterSource()
      .addValue("doc_id", docId)
      .addValue("doc_oid", docOid)
      .addTimestamp("ver_from_instant", document.getVersionFromInstant())
      .addTimestampNullFuture("ver_to_instant", document.getVersionToInstant())
      .addTimestamp("corr_from_instant", document.getCorrectionFromInstant())
      .addTimestampNullFuture("corr_to_instant", document.getCorrectionToInstant())
      .addValue("name", document.getSecurity().getName())
      .addValue("sec_type", document.getSecurity().getSecurityType());
    if (document.getSecurity() instanceof RawSecurity) {
      docArgs.addValue("detail_type", "R");
    } else if (document.getSecurity().getClass() == ManageableSecurity.class) {
      docArgs.addValue("detail_type", "M");
    } else {
      docArgs.addValue("detail_type", "D");
    }
    // the arguments for inserting into the idkey tables
    final List<DbMapSqlParameterSource> assocList = new ArrayList<DbMapSqlParameterSource>();
    final List<DbMapSqlParameterSource> idKeyList = new ArrayList<DbMapSqlParameterSource>();
    final String sqlSelectIdKey = getExtSqlBundle().getSql("SelectIdKey");
    for (ExternalId id : document.getSecurity().getExternalIdBundle()) {
      final DbMapSqlParameterSource assocArgs = new DbMapSqlParameterSource()
        .addValue("doc_id", docId)
        .addValue("key_scheme", id.getScheme().getName())
        .addValue("key_value", id.getValue());
      assocList.add(assocArgs);
      if (getJdbcTemplate().queryForList(sqlSelectIdKey, assocArgs).isEmpty()) {
        // select avoids creating unnecessary id, but id may still not be used
        final long idKeyId = nextId("sec_idkey_seq");
        final DbMapSqlParameterSource idkeyArgs = new DbMapSqlParameterSource()
          .addValue("idkey_id", idKeyId)
          .addValue("key_scheme", id.getScheme().getName())
          .addValue("key_value", id.getValue());
        idKeyList.add(idkeyArgs);
      }
    }
    final String sqlDoc = getExtSqlBundle().getSql("Insert", docArgs);
    final String sqlIdKey = getExtSqlBundle().getSql("InsertIdKey");
    final String sqlDoc2IdKey = getExtSqlBundle().getSql("InsertDoc2IdKey");
    getJdbcTemplate().update(sqlDoc, docArgs);
    getJdbcTemplate().batchUpdate(sqlIdKey, idKeyList.toArray(new DbMapSqlParameterSource[idKeyList.size()]));
    getJdbcTemplate().batchUpdate(sqlDoc2IdKey, assocList.toArray(new DbMapSqlParameterSource[assocList.size()]));
    // set the uniqueId
    final UniqueId uniqueId = createUniqueId(docOid, docId);
    document.getSecurity().setUniqueId(uniqueId);
    document.setUniqueId(uniqueId);
    
    // store the detail
    if (document.getSecurity() instanceof RawSecurity) {
      storeRawSecurityDetail((RawSecurity) document.getSecurity());
    } else {
      final SecurityMasterDetailProvider detailProvider = getDetailProvider();
      if (detailProvider != null) {
        detailProvider.storeSecurityDetail(document.getSecurity());
      }
    }
    
    // store attributes
    Map<String, String> attributes = new HashMap<String, String>(document.getSecurity().getAttributes());
    final List<DbMapSqlParameterSource> securityAttributeList = Lists.newArrayList();
    for (Map.Entry<String, String> entry : attributes.entrySet()) {
      final long securityAttrId = nextId("sec_security_attr_seq");
      final DbMapSqlParameterSource attributeArgs = new DbMapSqlParameterSource()
              .addValue("attr_id", securityAttrId)
              .addValue("security_id", docId)
              .addValue("security_oid", docOid)
              .addValue("key", entry.getKey())
              .addValue("value", entry.getValue());
      securityAttributeList.add(attributeArgs);
    }
    final String sqlAttributes = getExtSqlBundle().getSql("InsertAttributes");
    getJdbcTemplate().batchUpdate(sqlAttributes, securityAttributeList.toArray(new DbMapSqlParameterSource[securityAttributeList.size()]));
    return document;
  }

  private void storeRawSecurityDetail(RawSecurity security) {
    final DbMapSqlParameterSource rawArgs = new DbMapSqlParameterSource()
      .addValue("security_id", extractRowId(security.getUniqueId()))
      .addValue("raw_data", new SqlLobValue(security.getRawData(), getDialect().getLobHandler()), Types.BLOB);
    final String sqlRaw = getExtSqlBundle().getSql("InsertRaw", rawArgs);
    getJdbcTemplate().update(sqlRaw, rawArgs);
  }

  //-------------------------------------------------------------------------
  /**
   * Mapper from SQL rows to a SecurityDocument.
   */
  protected final class SecurityDocumentExtractor implements ResultSetExtractor<List<SecurityDocument>> {
    private long _lastDocId = -1;
    private ManageableSecurity _security;
    private List<SecurityDocument> _documents = new ArrayList<SecurityDocument>();

    @Override
    public List<SecurityDocument> extractData(final ResultSet rs) throws SQLException, DataAccessException {
      while (rs.next()) {
        final long docId = rs.getLong("DOC_ID");
        if (_lastDocId != docId) {
          _lastDocId = docId;
          buildSecurity(rs, docId);
        }
        final String idScheme = rs.getString("KEY_SCHEME");
        final String idValue = rs.getString("KEY_VALUE");
        if (idScheme != null && idValue != null) {
          ExternalId id = ExternalId.of(idScheme, idValue);
          _security.setExternalIdBundle(_security.getExternalIdBundle().withExternalId(id));
        }
        
        final String securityAttrKey = rs.getString("SECURITY_ATTR_KEY");
        final String securityAttrValue = rs.getString("SECURITY_ATTR_VALUE");
        if (securityAttrKey != null && securityAttrValue != null) {
          _security.addAttribute(securityAttrKey, securityAttrValue);
        }
      }
      return _documents;
    }

    private void buildSecurity(final ResultSet rs, final long docId) throws SQLException {
      final long docOid = rs.getLong("DOC_OID");
      final Timestamp versionFrom = rs.getTimestamp("VER_FROM_INSTANT");
      final Timestamp versionTo = rs.getTimestamp("VER_TO_INSTANT");
      final Timestamp correctionFrom = rs.getTimestamp("CORR_FROM_INSTANT");
      final Timestamp correctionTo = rs.getTimestamp("CORR_TO_INSTANT");
      final String name = rs.getString("NAME");
      final String type = rs.getString("SEC_TYPE");
      UniqueId uniqueId = createUniqueId(docOid, docId);
      String detailType = rs.getString("DETAIL_TYPE");
      if (detailType.equalsIgnoreCase("R")) {
        LobHandler lob = getDialect().getLobHandler();
        byte[] rawData = lob.getBlobAsBytes(rs, "RAW_DATA");
        _security = new RawSecurity(type, rawData);
        _security.setUniqueId(uniqueId);
        _security.setName(name);  
      } else {
        _security = new ManageableSecurity(uniqueId, name, type, ExternalIdBundle.EMPTY);
      }
      SecurityDocument doc = new SecurityDocument(_security);
      doc.setVersionFromInstant(DbDateUtils.fromSqlTimestamp(versionFrom));
      doc.setVersionToInstant(DbDateUtils.fromSqlTimestampNullFarFuture(versionTo));
      doc.setCorrectionFromInstant(DbDateUtils.fromSqlTimestamp(correctionFrom));
      doc.setCorrectionToInstant(DbDateUtils.fromSqlTimestampNullFarFuture(correctionTo));
      doc.setUniqueId(uniqueId);
      _documents.add(doc);
    }
  }

}
