/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.component;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.rest.AbstractDataResource;

/**
 * RESTful resource for exchanges.
 * <p>
 * The exchanges resource receives and processes RESTful calls to the exchange master.
 */
@Path("/components")
public class DataComponentsResource extends AbstractDataResource {

  /**
   * The repository.
   */
  private final ComponentRepository _repo;

  /**
   * Creates the resource around the repository.
   * 
   * @param repo  the component repository, not null
   */
  public DataComponentsResource(final ComponentRepository repo) {
    ArgumentChecker.notNull(repo, "repo");
    _repo = repo;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the repository.
   * 
   * @return the repository, not null
   */
  public ComponentRepository getComponentRepository() {
    return _repo;
  }

  //-------------------------------------------------------------------------
  @HEAD
  public Response status() {
    // simple GET to quickly return as a ping
    return Response.ok().build();
  }

  @GET
  public Response getComponentInfos() {
    Map<ComponentKey, Object> published = _repo.getPublishedMap();
    ComponentInfosMsg infos = new ComponentInfosMsg();
    for (ComponentKey key : published.keySet()) {
      infos.getInfos().add(_repo.getInfo(key.getType(), key.getClassifier()));
    }
    return Response.ok(infos).build();
  }

  @Path("{type}/{classifier}")
  public Object findComponent(@PathParam("type") String type, @PathParam("classifier") String classifier) {
    Map<ComponentKey, Object> published = _repo.getPublishedMap();
    for (ComponentKey key : published.keySet()) {
      if (key.getType().getSimpleName().equalsIgnoreCase(type) && key.getClassifier().equalsIgnoreCase(classifier)) {
        return published.get(key);
      }
    }
    return null;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds a URI to fetch all components.
   * 
   * @param baseUri  the base URI, not null
   * @return the URI, not null
   */
  public static URI uri(URI baseUri) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("/components");
    return bld.build();
  }

  /**
   * Builds a URI for a single component.
   * 
   * @param baseUri  the base URI, not null
   * @param info  the component info, not null
   * @return the URI, not null
   */
  public static URI uri(URI baseUri, ComponentInfo info) {
    UriBuilder bld = UriBuilder.fromUri(baseUri).path("/components/{type}/{classifier}");
    return bld.build(info.getType().getSimpleName(), info.getClassifier());
  }

}