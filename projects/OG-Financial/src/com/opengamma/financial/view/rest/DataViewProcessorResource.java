/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.view.rest;

import java.net.URI;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.fudgemsg.FudgeContext;

import com.opengamma.engine.view.ViewProcess;
import com.opengamma.engine.view.ViewProcessor;
import com.opengamma.engine.view.client.ViewClient;
import com.opengamma.financial.rest.AbstractRestfulJmsResultPublisherExpiryJob;
import com.opengamma.id.UniqueId;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.transport.jaxrs.FudgeRest;
import com.opengamma.util.jms.JmsConnector;

/**
 * RESTful resource for a {@link ViewProcessor}.
 */
@Path("/data/viewProcessors/{viewProcessorId}")
public class DataViewProcessorResource {

  /**
   * The period after which, if a view client has not been accessed, it may be shut down.
   */
  public static final long VIEW_CLIENT_TIMEOUT_MILLIS = 30000;

  //CSOFF: just constants
  public static final String PATH_DEFINITION_REPOSITORY = "definitions";
  public static final String PATH_LIVE_DATA_SOURCE_REGISTRY = "liveDataSourceRegistry";
  public static final String PATH_UNIQUE_ID = "id";
  public static final String PATH_CLIENTS = "clients";
  public static final String PATH_PROCESSES = "processes";
  public static final String PATH_CYCLES = "cycles";
  //CSON: just constants

  /**
   * The view processor.
   */
  private final ViewProcessor _viewProcessor;
  /**
   * The connection factory.
   */
  private final JmsConnector _jmsConnector;
  /**
   * The executor service.
   */
  private final ScheduledExecutorService _scheduler;
  /**
   * The stale view client expiry job. 
   */
  private final AbstractRestfulJmsResultPublisherExpiryJob<DataViewClientResource> _expiryJob;
  /**
   * The cycle manager.
   */
  private final AtomicReference<DataViewCycleManagerResource> _cycleManagerResource = new AtomicReference<DataViewCycleManagerResource>();
  /**
   * The view clients.
   */
  private final ConcurrentMap<UniqueId, DataViewClientResource> _createdViewClients = new ConcurrentHashMap<UniqueId, DataViewClientResource>();

  /**
   * Creates an instance.
   * 
   * @param viewProcessor  the view processor
   * @param jmsConnector  the JMS connector
   * @param fudgeContext  the Fudge context
   * @param scheduler  the scheduler, not null
   */
  public DataViewProcessorResource(ViewProcessor viewProcessor, JmsConnector jmsConnector, FudgeContext fudgeContext, ScheduledExecutorService scheduler) {
    _viewProcessor = viewProcessor;
    _jmsConnector = jmsConnector;
    _scheduler = scheduler;
    _expiryJob = new AbstractRestfulJmsResultPublisherExpiryJob<DataViewClientResource>(VIEW_CLIENT_TIMEOUT_MILLIS, scheduler) {
      @Override
      protected Collection<DataViewClientResource> getResources() {
        return _createdViewClients.values();
      }
    };
  }

  /**
   * Gets the viewProcessor field.
   * @return the viewProcessor
   */
  public ViewProcessor getViewProcessor() {
    return _viewProcessor;
  }

  //-------------------------------------------------------------------------
  @GET
  @Path(PATH_UNIQUE_ID)
  public Response getUniqueId() {
    return Response.ok(_viewProcessor.getUniqueId()).build();
  }

  @Path(PATH_DEFINITION_REPOSITORY)
  public DataViewDefinitionRepositoryResource getViewDefinitionRepository() {
    return new DataViewDefinitionRepositoryResource(_viewProcessor.getViewDefinitionRepository());
  }
  
  @Path(PATH_LIVE_DATA_SOURCE_REGISTRY)
  public LiveMarketDataSourceRegistryResource getLiveMarketDataSourceRegistry() {
    return new LiveMarketDataSourceRegistryResource(_viewProcessor.getLiveMarketDataSourceRegistry());
  }

  //-------------------------------------------------------------------------
  @Path(PATH_PROCESSES + "/{viewProcessId}")
  public DataViewProcessResource getViewProcess(@PathParam("viewProcessId") String viewProcessId) {
    ViewProcess view = _viewProcessor.getViewProcess(UniqueId.parse(viewProcessId));
    return new DataViewProcessResource(view);
  }

  //-------------------------------------------------------------------------
  @Path(PATH_CLIENTS + "/{viewClientId}")
  public DataViewClientResource getViewClient(@Context UriInfo uriInfo, @PathParam("viewClientId") String viewClientIdString) {
    UniqueId viewClientId = UniqueId.parse(viewClientIdString);
    DataViewClientResource viewClientResource = _createdViewClients.get(viewClientId);
    if (viewClientResource != null) {
      return viewClientResource;
    }
    ViewClient viewClient = _viewProcessor.getViewClient(viewClientId);
    URI viewProcessorUri = getViewProcessorUri(uriInfo);
    return createViewClientResource(viewClient, viewProcessorUri);
  }

  @POST
  @Path(PATH_CLIENTS)
  @Consumes(FudgeRest.MEDIA)
  public Response createViewClient(@Context UriInfo uriInfo, UserPrincipal user) {
    ViewClient client = _viewProcessor.createViewClient(user);
    URI viewProcessorUri = getViewProcessorUri(uriInfo);
    // Required for heartbeating, but also acts as an optimisation for getViewClient because view clients created
    // through the REST API should be accessed again through the same API, potentially many times.  
    DataViewClientResource viewClientResource = createViewClientResource(client, viewProcessorUri);
    _createdViewClients.put(client.getUniqueId(), viewClientResource);
    return Response.created(uriClient(uriInfo.getRequestUri(), client.getUniqueId())).build();
  }

  //-------------------------------------------------------------------------
  @Path(PATH_CYCLES)
  public DataViewCycleManagerResource getViewCycleManager(@Context UriInfo uriInfo) {
    return getOrCreateDataViewCycleManagerResource(getViewProcessorUri(uriInfo));
  }

  //-------------------------------------------------------------------------
  public static URI uriViewProcess(URI baseUri, UniqueId viewProcessId) {
    // WARNING: '/' characters could well appear in the view name
    // There is a bug(?) in UriBuilder where, even though segment() is meant to treat the item as a single path segment
    // and therefore encode '/' characters, it does not encode '/' characters which come from a variable substitution.
    return UriBuilder.fromUri(baseUri).path("processes").segment(viewProcessId.toString()).build();
  }

  public static URI uriClient(URI clientsBaseUri, UniqueId viewClientId) {
    return UriBuilder.fromUri(clientsBaseUri).segment(viewClientId.toString()).build();
  }

  private URI getViewProcessorUri(UriInfo uriInfo) {
    return UriBuilder.fromUri(uriInfo.getMatchedURIs().get(1)).build();
  }

  private DataViewCycleManagerResource getOrCreateDataViewCycleManagerResource(URI viewProcessorUri) {
    DataViewCycleManagerResource resource = _cycleManagerResource.get();
    if (resource == null) {
      URI baseUri = UriBuilder.fromUri(viewProcessorUri).path(PATH_CYCLES).build();
      DataViewCycleManagerResource newResource = new DataViewCycleManagerResource(baseUri, _viewProcessor.getViewCycleManager());
      if (_cycleManagerResource.compareAndSet(null, newResource)) {
        resource = newResource;
        DataViewCycleManagerResource.ReleaseExpiredReferencesRunnable task = newResource.createReleaseExpiredReferencesTask();
        task.setScheduler(_scheduler);
      } else {
        resource = _cycleManagerResource.get();
      }
    }
    return resource;
  }

  private DataViewClientResource createViewClientResource(ViewClient viewClient, URI viewProcessorUri) {
    DataViewCycleManagerResource cycleManagerResource = getOrCreateDataViewCycleManagerResource(viewProcessorUri);
    return new DataViewClientResource(viewClient, cycleManagerResource, _jmsConnector);
  }

}
