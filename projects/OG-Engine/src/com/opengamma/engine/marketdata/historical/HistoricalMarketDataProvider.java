/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.marketdata.historical;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.core.historicaltimeseries.HistoricalTimeSeries;
import com.opengamma.core.historicaltimeseries.HistoricalTimeSeriesSource;
import com.opengamma.engine.marketdata.AbstractMarketDataProvider;
import com.opengamma.engine.marketdata.MarketDataSnapshot;
import com.opengamma.engine.marketdata.availability.MarketDataAvailability;
import com.opengamma.engine.marketdata.availability.MarketDataAvailabilityProvider;
import com.opengamma.engine.marketdata.permission.MarketDataPermissionProvider;
import com.opengamma.engine.marketdata.permission.PermissiveMarketDataPermissionProvider;
import com.opengamma.engine.marketdata.spec.HistoricalMarketDataSpecification;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.livedata.UserPrincipal;
import com.opengamma.util.ArgumentChecker;

/**
 * Snapshot provider will always return the historical data on the date provided from the provided historical data source.
 */
public class HistoricalMarketDataProvider extends AbstractMarketDataProvider implements MarketDataAvailabilityProvider {
  
  private static final Logger s_logger = LoggerFactory.getLogger(HistoricalMarketDataProvider.class);
  
  private final HistoricalTimeSeriesSource _historicalTimeSeriesSource;
  private final String _timeSeriesResolverKey;
  private final String _timeSeriesFieldResolverKey;
  private final MarketDataPermissionProvider _permissionProvider;

  /**
   * Creates a new market data provider.
   * 
   * @param historicalTimeSeriesSource underlying source
   * @param timeSeriesResolverKey the source resolver key, or null to use the source default
   * @param fieldResolverKey the field name resolver resolution key, or null to use the resolver default
   */
  public HistoricalMarketDataProvider(final HistoricalTimeSeriesSource historicalTimeSeriesSource, final String timeSeriesResolverKey, 
      final String fieldResolverKey) {
    ArgumentChecker.notNull(historicalTimeSeriesSource, "historicalTimeSeriesSource");
    _historicalTimeSeriesSource = historicalTimeSeriesSource;
    _timeSeriesResolverKey = timeSeriesResolverKey;
    _timeSeriesFieldResolverKey = fieldResolverKey;
    _permissionProvider = new PermissiveMarketDataPermissionProvider();
  }
  
  public HistoricalMarketDataProvider(final HistoricalTimeSeriesSource historicalTimeSeriesSource) {
    this(historicalTimeSeriesSource, null, null);
  }

  //-------------------------------------------------------------------------
  @Override
  public void subscribe(UserPrincipal user, ValueRequirement valueRequirement) {
    subscribe(user, Collections.singleton(valueRequirement));
  }

  @Override
  public void subscribe(UserPrincipal user, Set<ValueRequirement> valueRequirements) {
    // Nothing to do, but we still need to acknowledge it.
    s_logger.debug("Added subscriptions to {}", valueRequirements);
    subscriptionSucceeded(valueRequirements);
  }
  
  @Override
  public void unsubscribe(UserPrincipal user, ValueRequirement valueRequirement) {
    unsubscribe(user, Collections.singleton(valueRequirement));
  }

  @Override
  public void unsubscribe(UserPrincipal user, Set<ValueRequirement> valueRequirements) {
    // Nothing to do
    s_logger.debug("Removed subscriptions from {}", valueRequirements);
  }
  
  //-------------------------------------------------------------------------
  @Override
  public MarketDataAvailabilityProvider getAvailabilityProvider() {
    return this;
  }

  @Override
  public MarketDataPermissionProvider getPermissionProvider() {
    return _permissionProvider;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean isCompatible(MarketDataSpecification marketDataSpec) {
    if (!(marketDataSpec instanceof HistoricalMarketDataSpecification)) {
      return false;
    }
    HistoricalMarketDataSpecification historicalSpec = (HistoricalMarketDataSpecification) marketDataSpec;
    return ObjectUtils.equals(getTimeSeriesResolverKey(), historicalSpec.getTimeSeriesResolverKey())
        && ObjectUtils.equals(getTimeSeriesFieldResolverKey(), historicalSpec.getTimeSeriesFieldResolverKey());
  }
  
  @Override
  public MarketDataSnapshot snapshot(MarketDataSpecification marketDataSpec) {
    HistoricalMarketDataSpecification historicalSpec = (HistoricalMarketDataSpecification) marketDataSpec;
    return new HistoricalMarketDataSnapshot(historicalSpec, getTimeSeriesSource());
  }

  //-------------------------------------------------------------------------

  @Override
  public MarketDataAvailability getAvailability(final ValueRequirement requirement) {
    final ExternalId identifier = requirement.getTargetSpecification().getIdentifier();
    final HistoricalTimeSeries hts = _historicalTimeSeriesSource.getHistoricalTimeSeries(requirement.getValueName(), ExternalIdBundle.of(identifier), getTimeSeriesResolverKey());
    return (hts != null) ? MarketDataAvailability.AVAILABLE : MarketDataAvailability.NOT_AVAILABLE;
  }
  
  //-------------------------------------------------------------------------
  private HistoricalTimeSeriesSource getTimeSeriesSource() {
    return _historicalTimeSeriesSource;
  }
  
  public String getTimeSeriesResolverKey() {
    return _timeSeriesResolverKey;
  }
  
  public String getTimeSeriesFieldResolverKey() {
    return _timeSeriesFieldResolverKey;
  }

}
