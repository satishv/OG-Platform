/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.position.rest;

import org.fudgemsg.FudgeContext;

import com.opengamma.core.change.BasicChangeManager;
import com.opengamma.core.change.ChangeManager;
import com.opengamma.core.position.Portfolio;
import com.opengamma.core.position.PortfolioNode;
import com.opengamma.core.position.Position;
import com.opengamma.core.position.PositionSource;
import com.opengamma.core.position.Trade;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.transport.jaxrs.RestClient;
import com.opengamma.transport.jaxrs.RestTarget;
import com.opengamma.util.ArgumentChecker;

/**
 * Provides access to a remote {@link PositionSource}.
 */
public class RemotePositionSource implements PositionSource {

  /**
   * The base URI to call.
   */
  private final RestTarget _target;
  /**
   * The client API.
   */
  private final RestClient _client;
  /**
   * The change manager.
   */
  private final ChangeManager _changeManager;

  public RemotePositionSource(final FudgeContext fudgeContext, final RestTarget restTarget) {
    this(fudgeContext, restTarget, new BasicChangeManager());
  }
  
  public RemotePositionSource(final FudgeContext fudgeContext, final RestTarget restTarget, ChangeManager changeManager) {
    ArgumentChecker.notNull(fudgeContext, "fudgeContext");
    ArgumentChecker.notNull(restTarget, "restTarget");
    ArgumentChecker.notNull(changeManager, "changeManager");
    _client = RestClient.getInstance(fudgeContext, null);
    _target = restTarget;
    _changeManager = changeManager;
  }

  //-------------------------------------------------------------------------
  @Override
  public Portfolio getPortfolio(UniqueId uniqueId) {
    ArgumentChecker.notNull(uniqueId, "uniqueId");
    Portfolio result = _client.getSingleValue(Portfolio.class, DataPositionSourceResource.targetPortfolio(_target, uniqueId), "portfolio");
    return result;
  }

  @Override
  public Portfolio getPortfolio(ObjectId objectId, VersionCorrection versionCorrection) {
    ArgumentChecker.notNull(objectId, "objectId");
    ArgumentChecker.notNull(versionCorrection, "versionCorrection");
    Portfolio result = _client.getSingleValue(Portfolio.class, DataPositionSourceResource.targetPortfolio(_target, objectId, versionCorrection), "portfolio");
    return result;
  }

  @Override
  public PortfolioNode getPortfolioNode(UniqueId uniqueId) {
    ArgumentChecker.notNull(uniqueId, "uniqueId");
    PortfolioNode result = _client.getSingleValue(PortfolioNode.class, DataPositionSourceResource.targetPortfolioNode(_target, uniqueId), "node");
    return result;
  }

  @Override
  public Position getPosition(UniqueId uniqueId) {
    ArgumentChecker.notNull(uniqueId, "uniqueId");
    Position result = _client.getSingleValue(Position.class, DataPositionSourceResource.targetPosition(_target, uniqueId), "position");
    return result;
  }

  @Override
  public Trade getTrade(UniqueId uniqueId) {
    ArgumentChecker.notNull(uniqueId, "uniqueId");
    Trade result = _client.getSingleValue(Trade.class, DataPositionSourceResource.targetTrade(_target, uniqueId), "trade");
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public ChangeManager changeManager() {
    return _changeManager;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a string summary of this object.
   * 
   * @return the string summary, not null
   */
  @Override
  public String toString() {
    return getClass().getSimpleName() + "[" + _target + "]";
  }

}
