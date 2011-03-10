/**
 * Copyright (C) 2009 - 2011 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.option.pricing.fourier;

import static com.opengamma.math.ComplexMathUtils.add;
import static com.opengamma.math.ComplexMathUtils.divide;
import static com.opengamma.math.ComplexMathUtils.exp;
import static com.opengamma.math.ComplexMathUtils.subtract;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import com.opengamma.financial.model.option.pricing.analytic.formula.BlackFunctionData;
import com.opengamma.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.number.ComplexNumber;

/**
 * 
 */
public class EuropeanPriceIntegrand1 {
  private final CharacteristicExponent1 _ce;
  private final double _alpha;
  private final boolean _useVarianceReduction;

  public EuropeanPriceIntegrand1(final CharacteristicExponent1 ce, final double alpha, final boolean useVarianceReduction) {
    Validate.notNull(ce, "characteristic exponent");
    _ce = new MeanCorrectedCharacteristicExponent1(ce);
    _alpha = alpha;
    _useVarianceReduction = useVarianceReduction;
  }

  public Function1D<Double, Double> getFunction(final BlackFunctionData data, final EuropeanVanillaOption option) {
    Validate.notNull(data, "data");
    Validate.notNull(option, "option");
    final double t = option.getTimeToExpiry();
    final Function1D<ComplexNumber, ComplexNumber> characteristicFunction = _ce.getFunction(t);
    final double k = Math.log(option.getStrike() / data.getForward());
    final double blackVol = data.getBlackVolatility();
    final CharacteristicExponent1 gaussian = _useVarianceReduction ? new GaussianCharacteristicExponent1(-0.5 * blackVol * blackVol, blackVol) : null;
    final Function1D<ComplexNumber, ComplexNumber> gaussianFunction = _useVarianceReduction ? gaussian.getFunction(t) : null;
    return new Function1D<Double, Double>() {

      @Override
      public Double evaluate(final Double x) {
        final ComplexNumber res = getIntegrand(x, characteristicFunction, gaussianFunction, k);
        return res.getReal();
      }
    };
  }

  private ComplexNumber getIntegrand(final double x, final Function1D<ComplexNumber, ComplexNumber> ce, final Function1D<ComplexNumber, ComplexNumber> gaussian, final double k) {
    final ComplexNumber z = new ComplexNumber(x, -1 - _alpha);
    final ComplexNumber num1 = exp(add(new ComplexNumber(0, -x * k), ce.evaluate(z)));
    final ComplexNumber num2 = gaussian == null ? new ComplexNumber(0.0) : exp(add(new ComplexNumber(0, -x * k), gaussian.evaluate(z)));
    final ComplexNumber denom = new ComplexNumber(_alpha * (1 + _alpha) - x * x, (2 * _alpha + 1) * x);
    final ComplexNumber res = divide(subtract(num1, num2), denom);
    return res;
  }

  public CharacteristicExponent1 getCharacteristicExponent() {
    return _ce;
  }

  public double getAlpha() {
    return _alpha;
  }

  public boolean useVarianceReduction() {
    return _useVarianceReduction;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(_alpha);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + _ce.hashCode();
    result = prime * result + (_useVarianceReduction ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final EuropeanPriceIntegrand1 other = (EuropeanPriceIntegrand1) obj;
    if (Double.doubleToLongBits(_alpha) != Double.doubleToLongBits(other._alpha)) {
      return false;
    }
    if (!ObjectUtils.equals(_ce, other._ce)) {
      return false;
    }
    return _useVarianceReduction == other._useVarianceReduction;
  }
}