/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.marketdatasnapshot.impl;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertSame;

import javax.ws.rs.core.Response;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.core.marketdatasnapshot.impl.ManageableMarketDataSnapshot;
import com.opengamma.id.ObjectId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.master.marketdatasnapshot.MarketDataSnapshotDocument;
import com.opengamma.master.marketdatasnapshot.MarketDataSnapshotMaster;
import com.sun.jersey.api.client.ClientResponse.Status;

/**
 * Tests DataMarketDataSnapshotResource.
 */
public class DataMarketDataSnapshotResourceTest {

  private static final ObjectId OID = ObjectId.of("Test", "A");
  private MarketDataSnapshotMaster _underlying;
  private DataMarketDataSnapshotResource _resource;

  @BeforeMethod
  public void setUp() {
    _underlying = mock(MarketDataSnapshotMaster.class);
    _resource = new DataMarketDataSnapshotResource(new DataMarketDataSnapshotsResource(_underlying), OID.getObjectId());
  }

  //-------------------------------------------------------------------------
  @Test
  public void testGetMarketDataSnapshot() {
    final ManageableMarketDataSnapshot target = new ManageableMarketDataSnapshot();
    target.setBasisViewName("Basis");
    
    final MarketDataSnapshotDocument result = new MarketDataSnapshotDocument(target);
    when(_underlying.get(OID, VersionCorrection.LATEST)).thenReturn(result);
    
    Response test = _resource.get(null, null);
    assertEquals(Status.OK.getStatusCode(), test.getStatus());
    assertSame(result, test.getEntity());
  }

  @Test
  public void testUpdateMarketDataSnapshot() {
    final ManageableMarketDataSnapshot target = new ManageableMarketDataSnapshot();
    target.setBasisViewName("Basis");
    final MarketDataSnapshotDocument request = new MarketDataSnapshotDocument(target);
    request.setUniqueId(OID.atLatestVersion());
    
    final MarketDataSnapshotDocument result = new MarketDataSnapshotDocument(target);
    result.setUniqueId(OID.atLatestVersion());
    when(_underlying.update(same(request))).thenReturn(result);
    
    Response test = _resource.put(request);
    assertEquals(Status.OK.getStatusCode(), test.getStatus());
    assertSame(result, test.getEntity());
  }

  @Test
  public void testDeleteMarketDataSnapshot() {
    Response test = _resource.delete();
    verify(_underlying).remove(OID.atLatestVersion());
    assertEquals(Status.NO_CONTENT.getStatusCode(), test.getStatus());
  }

}
