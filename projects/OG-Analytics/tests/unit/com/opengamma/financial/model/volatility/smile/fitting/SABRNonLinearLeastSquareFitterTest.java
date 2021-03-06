/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.model.volatility.smile.fitting;

import org.testng.annotations.Test;

import com.opengamma.financial.model.volatility.smile.function.SABRHaganVolatilityFunction;

/**
 * 
 */
public class SABRNonLinearLeastSquareFitterTest extends LeastSquareSmileFitterTestCase {
  private static final SABRNonLinearLeastSquareFitter FITTER = new SABRNonLinearLeastSquareFitter(new SABRHaganVolatilityFunction());
  private static final double[] INITIAL_VALUES = new double[] {0.5, 1, 0.2, 0};

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFormula() {
    new SABRNonLinearLeastSquareFitter(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeATMVol() {
    FITTER.getFitResult(OPTIONS, FLAT_DATA, ERRORS, INITIAL_VALUES, FIXED, -0.4, true);
  }

  @Override
  protected LeastSquareSmileFitter getFitter() {
    return FITTER;
  }

  @Override
  protected double[] getInitialValues() {
    return INITIAL_VALUES;
  }

}
