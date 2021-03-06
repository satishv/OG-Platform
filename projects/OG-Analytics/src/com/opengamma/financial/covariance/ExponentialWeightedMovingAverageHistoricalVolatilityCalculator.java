/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.covariance;

import java.util.Iterator;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.financial.timeseries.returns.ContinuouslyCompoundedTimeSeriesReturnCalculator;
import com.opengamma.financial.timeseries.returns.TimeSeriesReturnCalculator;
import com.opengamma.util.CalculationMode;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;

/**
 * 
 * Exponentially weighted moving average (EWMA) volatility calculations put variable weight on the values in a time series. The weight is controlled by
 * a parameter {@latex.inline $\\lambda$} which can take any positive value: in most markets, the most suitable range is between 0.75 and 1. 
 * <p>
 * The exponential moving average of a time series is given by:
 * {@latex.ilb %preamble{\\usepackage{amsmath}}
 * \\begin{eqnarray*}
 * \\frac{x_{t-1} + \\lambda x_{t-2} + \\lambda^2 x_{t-3} + \\dots + \\lambda^{n-1} x_{t-n}}{1 + \\lambda + \\lambda^2 + \\dots + \\lambda^{n-1}}
 * \\end{eqnarray*}} 
 * where {@latex.inline $x_i$} is the {@latex.inline %preamble{\\usepackage{amsmath}} $i^\\text{th}$} value in the time series and {@latex.inline $\\lambda$} is the weight.
 * <p>
 * The exponential weighted volatility is:
 * {@latex.ilb %preamble{\\usepackage{amsmath}}
 * \\begin{eqnarray*}
 * \\sigma_t = \\sqrt{\\lambda \\sigma_{t-1}^2 + (1 - \\lambda)r_t^2}
 * \\end{eqnarray*}}
 * where {@latex.inline $\\sigma_{t-1}$} is the previous volatility calculation and {@latex.inline $r_t$} is the continuously compounded return over a single period.
 * As with other historical volatility calculations, the volatility can be annualized by scaling by the square root of the number of periods in a year. 
 */
public class ExponentialWeightedMovingAverageHistoricalVolatilityCalculator extends HistoricalVolatilityCalculator {
  private static final Logger s_logger = LoggerFactory.getLogger(ExponentialWeightedMovingAverageHistoricalVolatilityCalculator.class);
  private final TimeSeriesReturnCalculator _returnCalculator;
  private final double _lambda;
  private final double _lambdaM1;

  /**
   * Although the return calculator can be any {@link TimeSeriesReturnCalculator}, to obtain correct results a {@link ContinuouslyCompoundedTimeSeriesReturnCalculator} should be 
   * used. The calculation mode is set to be the default (strict). Although the weight parameter can take any positive value, for most use the range should be {@latex.inline $\\lambda < 1$};
   * if a value higher outside of this range is used then greater weight will be placed on older return values. 
   * @param lambda The weight parameter
   * @param returnCalculator The return calculator
   * @throws IllegalArgumentException If the calculator is null or if {@latex.inline $\\lambda < 0$}
   */
  public ExponentialWeightedMovingAverageHistoricalVolatilityCalculator(final double lambda, final TimeSeriesReturnCalculator returnCalculator) {
    this(lambda, returnCalculator, getDefaultCalculationMode());
  }

  /**
   * Although the return calculator can be any {@link TimeSeriesReturnCalculator}, to obtain correct results a {@link ContinuouslyCompoundedTimeSeriesReturnCalculator} should be 
   * used. Although the weight parameter can take any positive value, for most use the range should be {@latex.inline $\\lambda < 1$}; if a value higher outside of this range is used then 
   * greater weight will be placed on older return values. 
   * @param lambda The weight parameter
   * @param returnCalculator The return calculator
   * @param mode The calculation mode
   * @throws IllegalArgumentException If the calculator is null or if {@latex.inline $\\lambda < 0$}
   */
  public ExponentialWeightedMovingAverageHistoricalVolatilityCalculator(final double lambda, final TimeSeriesReturnCalculator returnCalculator, final CalculationMode mode) {
    super(mode);
    Validate.notNull(returnCalculator, "return calculator");
    Validate.notNull(mode, "calculation mode");
    checkLambda(lambda);
    _lambda = lambda;
    _lambdaM1 = 1 - lambda;
    _returnCalculator = returnCalculator;
  }

  private void checkLambda(final double lambda) {
    if (lambda < 0) {
      throw new IllegalArgumentException("Lambda must be positive");
    }
    if (lambda > 1) {
      s_logger.warn("Weight for EWMA series is greater than one: this is probably not what was intended");
    }
  }

  /**
   * @param x The array of price time series. The first time series should be the price; any other arrays are assumed to be a timeseries of dividend payments.
   * @return The exponential weighted historical volatility
   * @throws IllegalArgumentException If x is null, empty or if the first element of the array is null; if the number of values in the time series is less than three; if the 
   * dates in the different time series do not coincide
   */
  @Override
  public Double evaluate(final LocalDateDoubleTimeSeries... x) {
    testTimeSeries(x, 3);
    final LocalDateDoubleTimeSeries returnTS = _returnCalculator.evaluate(x);
    final Iterator<Double> iter = returnTS.valuesIterator();
    double returnValue = iter.next();
    double variance = returnValue * returnValue;
    while (iter.hasNext()) {
      returnValue = iter.next();
      variance = _lambda * variance + _lambdaM1 * returnValue * returnValue;
    }
    return Math.sqrt(variance);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(_lambda);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + ((_returnCalculator == null) ? 0 : _returnCalculator.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ExponentialWeightedMovingAverageHistoricalVolatilityCalculator other = (ExponentialWeightedMovingAverageHistoricalVolatilityCalculator) obj;
    if (Double.doubleToLongBits(_lambda) != Double.doubleToLongBits(other._lambda)) {
      return false;
    }
    return ObjectUtils.equals(_returnCalculator, other._returnCalculator);
  }
}
