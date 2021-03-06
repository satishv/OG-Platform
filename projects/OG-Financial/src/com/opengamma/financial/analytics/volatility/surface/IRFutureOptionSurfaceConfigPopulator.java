/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.volatility.surface;

import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.master.config.ConfigDocument;
import com.opengamma.master.config.ConfigMaster;
import com.opengamma.master.config.ConfigMasterUtils;
import com.opengamma.util.money.Currency;

/**
 * 
 */
public class IRFutureOptionSurfaceConfigPopulator {

  public IRFutureOptionSurfaceConfigPopulator(final ConfigMaster configMaster) {
    populateVolatilitySurfaceConfigMaster(configMaster);
  }

  public static ConfigMaster populateVolatilitySurfaceConfigMaster(final ConfigMaster configMaster) {
    populateVolatilitySurfaceSpecifications(configMaster);
    populateVolatilitySurfaceDefinitions(configMaster);
    return configMaster;
  }

  private static void populateVolatilitySurfaceDefinitions(final ConfigMaster configMaster) {
    final Integer[] futureOptionNumbers = new Integer[18];
    for (int i = 0; i < 18; i++) {
      futureOptionNumbers[i] = i + 1;
    }
    final Double[] strikes = new Double[24];
    double strike = 99.875;
    for (int i = 0; i < 24; i++) {
      strikes[i] = strike;
      strike -= 0.125; // quoted option strikes decrease by this amount
    }
    final VolatilitySurfaceDefinition<Integer, Double> usVolSurfaceDefinition = new VolatilitySurfaceDefinition<Integer, Double>("DEFAULT_IR_FUTURE_OPTION",
        Currency.USD, futureOptionNumbers, strikes);
    final FuturePriceCurveDefinition<Integer> usFuturePriceCurveDefinition = new FuturePriceCurveDefinition<Integer>("DEFAULT_IR_FUTURE_PRICE", Currency.USD, futureOptionNumbers);
    ConfigMasterUtils.storeByName(configMaster, makeConfigDocument(usVolSurfaceDefinition));
    ConfigMasterUtils.storeByName(configMaster, makeConfigDocument(usFuturePriceCurveDefinition));
  }

  private static ConfigDocument<VolatilitySurfaceDefinition<Integer, Double>> makeConfigDocument(final VolatilitySurfaceDefinition<Integer, Double> definition) {
    final ConfigDocument<VolatilitySurfaceDefinition<Integer, Double>> configDocument = new ConfigDocument<VolatilitySurfaceDefinition<Integer, Double>>(VolatilitySurfaceDefinition.class);
    configDocument.setName(definition.getName());
    configDocument.setValue(definition);
    return configDocument;
  }

  private static ConfigDocument<VolatilitySurfaceSpecification> makeConfigDocument(final VolatilitySurfaceSpecification specification) {
    final ConfigDocument<VolatilitySurfaceSpecification> configDocument = new ConfigDocument<VolatilitySurfaceSpecification>(VolatilitySurfaceSpecification.class);
    configDocument.setName(specification.getName());
    configDocument.setValue(specification);
    return configDocument;
  }

  private static ConfigDocument<FuturePriceCurveDefinition<Integer>> makeConfigDocument(final FuturePriceCurveDefinition<Integer> definition) {
    final ConfigDocument<FuturePriceCurveDefinition<Integer>> configDocument = new ConfigDocument<FuturePriceCurveDefinition<Integer>>(FuturePriceCurveDefinition.class);
    configDocument.setName(definition.getName());
    configDocument.setValue(definition);
    return configDocument;
  }

  private static ConfigDocument<FuturePriceCurveSpecification> makeConfigDocument(final FuturePriceCurveSpecification specification) {
    final ConfigDocument<FuturePriceCurveSpecification> configDocument = new ConfigDocument<FuturePriceCurveSpecification>(FuturePriceCurveSpecification.class);
    configDocument.setName(specification.getName());
    configDocument.setValue(specification);
    return configDocument;
  }

  private static void populateVolatilitySurfaceSpecifications(final ConfigMaster configMaster) {
    final SurfaceInstrumentProvider<Number, Double> surfaceInstrumentProvider = new BloombergIRFutureOptionVolatilitySurfaceInstrumentProvider("ED", "Comdty",
        MarketDataRequirementNames.IMPLIED_VOLATILITY, 97.775);
    final FuturePriceCurveInstrumentProvider<Number> curveInstrumentProvider = new BloombergIRFuturePriceCurveInstrumentProvider("ED", "Comdty", MarketDataRequirementNames.MARKET_VALUE);
    final VolatilitySurfaceSpecification usVolSurfaceDefinition = new VolatilitySurfaceSpecification("DEFAULT_IR_FUTURE_OPTION", Currency.USD, surfaceInstrumentProvider);
    final FuturePriceCurveSpecification usFutureCurveDefinition = new FuturePriceCurveSpecification("DEFAULT_IR_FUTURE_PRICE", Currency.USD, curveInstrumentProvider);
    ConfigMasterUtils.storeByName(configMaster, makeConfigDocument(usVolSurfaceDefinition));
    ConfigMasterUtils.storeByName(configMaster, makeConfigDocument(usFutureCurveDefinition));
  }
}
