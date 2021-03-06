/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.equity.variance;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.equity.AbstractEquityDerivativeVisitor;
import com.opengamma.financial.equity.EquityDerivative;
import com.opengamma.financial.equity.future.derivative.EquityFuture;
import com.opengamma.financial.equity.future.derivative.EquityIndexDividendFuture;
import com.opengamma.financial.equity.variance.derivative.VarianceSwap;
import com.opengamma.financial.equity.variance.pricing.VarianceSwapStaticReplication;
import com.opengamma.financial.equity.variance.pricing.VarianceSwapStaticReplication.StrikeParameterization;

/**
 * 
 */
public class VarianceSwapPresentValueCalculator extends AbstractEquityDerivativeVisitor<VarianceSwapDataBundle, Double> {

  private static final VarianceSwapPresentValueCalculator s_instance = new VarianceSwapPresentValueCalculator();

  public static VarianceSwapPresentValueCalculator getInstance() {
    return s_instance;
  }

  public VarianceSwapPresentValueCalculator() {
  }

  @Override
  public Double visitVarianceSwap(final VarianceSwap derivative, final VarianceSwapDataBundle market) {
    Validate.notNull(market);
    Validate.notNull(derivative);
    final StrikeParameterization strikeType = market.getVolatilitySurface().getStrikeParameterisation();
    VarianceSwapStaticReplication pricer = new VarianceSwapStaticReplication(strikeType);
    return pricer.presentValue(derivative, market);
  }

  @Override
  public Double visitVarianceSwap(final VarianceSwap derivative) {
    throw new UnsupportedOperationException("This visitor (" + this.getClass() + ") does not support a VarianceSwap without a VarianceSwapDataBundle");
  }

  @Override
  public Double visit(EquityDerivative derivative) {
    if (derivative instanceof VarianceSwap) {
      return visitVarianceSwap((VarianceSwap) derivative);
    }
    throw new UnsupportedOperationException("This visitor (" + this.getClass() + ") does not support visit(EquityDerivative). See Type Hierarchy of EquityDerivativeVisitor.");
  }

  @Override
  public Double visitEquityFuture(EquityFuture equityFuture) {
    throw new UnsupportedOperationException("This visitor (" + this.getClass() + ") does not support visitEquityFuture(). Try EquityFuturesPresentValueCalculator");
  }

  @Override
  public Double visitEquityIndexDividendFuture(EquityIndexDividendFuture equityIndexDividendFuture) {
    throw new UnsupportedOperationException("This visitor (" + this.getClass() + ") does not support visitEquityIndexDividendFuture(). Try EquityFuturesPresentValueCalculator");
  }

}
