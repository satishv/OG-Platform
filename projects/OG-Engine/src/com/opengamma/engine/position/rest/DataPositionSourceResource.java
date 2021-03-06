/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.position.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsgEnvelope;
import org.fudgemsg.MutableFudgeMsg;
import org.fudgemsg.mapping.FudgeSerializer;

import com.opengamma.core.position.Portfolio;
import com.opengamma.core.position.PortfolioNode;
import com.opengamma.core.position.Position;
import com.opengamma.core.position.PositionSource;
import com.opengamma.core.position.Trade;
import com.opengamma.id.ObjectId;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.transport.jaxrs.RestTarget;
import com.opengamma.util.ArgumentChecker;

/**
 * RESTful resource for a position source.
 */
@Path("/positionSource")
public class DataPositionSourceResource {
  // TODO: indirect through another container to allow multiple position sources to be published

  /**
   * The injected position source.
   */
  private final PositionSource _positionSource;

  /**
   * The injected Fudge context.
   */
  private final FudgeContext _fudgeContext;

  /**
   * Creates the resource.
   * 
   * @param fudgeContext  the Fudge context, not null
   * @param positionSource  the position source, not null
   */
  public DataPositionSourceResource(final FudgeContext fudgeContext, final PositionSource positionSource) {
    ArgumentChecker.notNull(fudgeContext, "fudgeContext");
    ArgumentChecker.notNull(positionSource, "positionSource");
    _fudgeContext = fudgeContext;
    _positionSource = positionSource;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the position source.
   * 
   * @return the position source, not null
   */
  public PositionSource getPositionSource() {
    return _positionSource;
  }

  protected FudgeContext getFudgeContext() {
    return _fudgeContext;
  }

  //-------------------------------------------------------------------------
  @GET
  @Path("portfolios/{uniqueId}")
  public FudgeMsgEnvelope getPortfolio(@PathParam("uniqueId") String uniqueIdStr) {
    UniqueId uniqueId = UniqueId.parse(uniqueIdStr);
    Portfolio result = getPositionSource().getPortfolio(uniqueId);
    if (result == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    final FudgeSerializer serializer = new FudgeSerializer(getFudgeContext());
    MutableFudgeMsg msg = serializer.newMessage();
    serializer.addToMessage(msg, "portfolio", null, result);
    return new FudgeMsgEnvelope(msg);
  }

  @GET
  @Path("portfolios/{objectId}/{versionAsOf}/{correctedTo}")
  public FudgeMsgEnvelope getSecurity(@PathParam("objectId") String objectIdStr, @PathParam("versionAsOf") String versionAsOfStr, @PathParam("correctedTo") String correctedToStr) {
    final ObjectId objectId = ObjectId.parse(objectIdStr);
    final VersionCorrection versionCorrection = VersionCorrection.parse(versionAsOfStr, correctedToStr);
    Portfolio result = getPositionSource().getPortfolio(objectId, versionCorrection);
    if (result == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    final FudgeSerializer serializer = new FudgeSerializer(getFudgeContext());
    final MutableFudgeMsg msg = serializer.newMessage();
    serializer.addToMessageWithClassHeaders(msg, "portfolio", null, result);
    return new FudgeMsgEnvelope(msg);
  }

  @GET
  @Path("nodes/{uniqueId}")
  public FudgeMsgEnvelope getNode(@PathParam("uniqueId") String uniqueIdStr) {
    UniqueId uniqueId = UniqueId.parse(uniqueIdStr);
    PortfolioNode result = getPositionSource().getPortfolioNode(uniqueId);
    if (result == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    final FudgeSerializer serializer = new FudgeSerializer(getFudgeContext());
    MutableFudgeMsg msg = serializer.newMessage();
    serializer.addToMessage(msg, "node", null, result);
    return new FudgeMsgEnvelope(msg);
  }

  @GET
  @Path("positions/{uniqueId}")
  public FudgeMsgEnvelope getPosition(@PathParam("uniqueId") String uniqueIdStr) {
    UniqueId uniqueId = UniqueId.parse(uniqueIdStr);
    Position result = getPositionSource().getPosition(uniqueId);
    if (result == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    final FudgeSerializer serializer = new FudgeSerializer(getFudgeContext());
    MutableFudgeMsg msg = serializer.newMessage();
    serializer.addToMessage(msg, "position", null, result);
    return new FudgeMsgEnvelope(msg);
  }

  @GET
  @Path("trades/{uniqueId}")
  public FudgeMsgEnvelope getTrade(@PathParam("uniqueId") String uniqueIdStr) {
    UniqueId uniqueId = UniqueId.parse(uniqueIdStr);
    Trade result = getPositionSource().getTrade(uniqueId);
    if (result == null) {
      throw new WebApplicationException(Response.Status.NOT_FOUND);
    }
    final FudgeSerializer serializer = new FudgeSerializer(getFudgeContext());
    MutableFudgeMsg msg = serializer.newMessage();
    serializer.addToMessage(msg, "trade", null, result);
    return new FudgeMsgEnvelope(msg);
  }

  //-------------------------------------------------------------------------
  public static RestTarget targetPortfolio(final RestTarget target, final UniqueId uniqueId) {
    return target.resolveBase("portfolios").resolve(uniqueId.toString());
  }

  public static RestTarget targetPortfolio(final RestTarget target, final ObjectId objectId, final VersionCorrection versionCorrection) {
    return target.resolveBase("portfolios").resolveBase(objectId.toString())
      .resolveBase(versionCorrection.getVersionAsOfString()).resolve(versionCorrection.getCorrectedToString());
  }

  public static RestTarget targetPortfolioNode(final RestTarget target, final UniqueId uniqueId) {
    return target.resolveBase("nodes").resolve(uniqueId.toString());
  }

  public static RestTarget targetPosition(final RestTarget target, final UniqueId uniqueId) {
    return target.resolveBase("positions").resolve(uniqueId.toString());
  }

  public static RestTarget targetTrade(final RestTarget target, final UniqueId uniqueId) {
    return target.resolveBase("trades").resolve(uniqueId.toString());
  }

}
