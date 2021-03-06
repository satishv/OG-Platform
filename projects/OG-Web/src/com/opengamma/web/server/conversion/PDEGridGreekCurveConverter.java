/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.web.server.conversion;

import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.greeks.PDEGreekResultCollection;

/**
 * 
 */
public class PDEGridGreekCurveConverter implements ResultConverter<PDEGreekResultCollection> {

  @Override
  public Object convertForDisplay(ResultConverterCache context, ValueSpecification valueSpec, PDEGreekResultCollection value, ConversionMode mode) {
    return null;
  }

  @Override
  public Object convertForHistory(ResultConverterCache context, ValueSpecification valueSpec, PDEGreekResultCollection value) {
    return null;
  }

  @Override
  public String convertToText(ResultConverterCache context, ValueSpecification valueSpec, PDEGreekResultCollection value) {
    
    valueSpec.getValueName();
    return null;
  }

  @Override
  public String getFormatterName() {
    return "PDE_GRID_GREEK";
  }

}
