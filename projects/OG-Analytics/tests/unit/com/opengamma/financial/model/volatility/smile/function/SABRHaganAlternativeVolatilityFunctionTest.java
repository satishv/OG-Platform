/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.model.volatility.smile.function;


/**
 * 
 */
public class SABRHaganAlternativeVolatilityFunctionTest extends SABRVolatilityFunctionTestCase {
  private static final SABRHaganAlternativeVolatilityFunction FUNCTION = new SABRHaganAlternativeVolatilityFunction();

  @Override
  protected VolatilityFunctionProvider<SABRFormulaData> getFunction() {
    return FUNCTION;
  }

}
