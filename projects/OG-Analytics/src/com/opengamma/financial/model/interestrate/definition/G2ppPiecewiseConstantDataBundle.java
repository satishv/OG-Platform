/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.interestrate.definition;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.YieldCurveBundle;

/**
 * Class describing the data required to price interest rate derivatives with G2++ (curves and parameters).
 */
public class G2ppPiecewiseConstantDataBundle extends YieldCurveBundle {

  /**
   * The G2++ model parameters.
   */
  private final G2ppPiecewiseConstantParameters _parameters;

  /**
   * Constructor from G2++ parameters and curve bundle.
   * @param g2ppParameters The G2++ model parameters.
   * @param curves Curve bundle.
   */
  public G2ppPiecewiseConstantDataBundle(final G2ppPiecewiseConstantParameters g2ppParameters, final YieldCurveBundle curves) {
    super(curves);
    Validate.notNull(g2ppParameters, "G2++ parameters");
    _parameters = g2ppParameters;
  }

  /**
   * Gets the G2++ parameters.
   * @return The parameters.
   */
  public G2ppPiecewiseConstantParameters getG2ppParameter() {
    return _parameters;
  }

}
