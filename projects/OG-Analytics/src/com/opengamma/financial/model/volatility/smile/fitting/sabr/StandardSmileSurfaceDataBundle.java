/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.volatility.smile.fitting.sabr;

import java.util.Arrays;

import org.apache.commons.lang.ObjectUtils;

import com.opengamma.financial.model.interestrate.curve.ForwardCurve;
import com.opengamma.math.curve.InterpolatedDoublesCurve;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.util.ArgumentChecker;

/**
 * 
 */
public class StandardSmileSurfaceDataBundle extends SmileSurfaceDataBundle {
  private final double[] _forwards;
  private final double[] _expiries;
  private final double[][] _strikes;
  private final double[][] _impliedVols;
  private final ForwardCurve _forwardCurve;
  private final int _nExpiries;
  private final boolean _isCallData;

  public StandardSmileSurfaceDataBundle(final double[] forwards, final double[] expiries, final double[][] strikes, final double[][] impliedVols, final boolean isCallData,
      final Interpolator1D forwardCurveInterpolator) {
    ArgumentChecker.notNull(forwards, "forwards");
    ArgumentChecker.notNull(expiries, "expiries");
    ArgumentChecker.notNull(strikes, "strikes");
    ArgumentChecker.notNull(impliedVols, "implied volatilities");
    ArgumentChecker.notNull(forwardCurveInterpolator, "forward curve interpolator");
    _nExpiries = expiries.length;
    ArgumentChecker.isTrue(_nExpiries == forwards.length, "forwards wrong length; have {}, need {}", forwards.length, _nExpiries);
    ArgumentChecker.isTrue(_nExpiries == strikes.length, "strikes wrong length; have {}, need {}", strikes.length, _nExpiries);
    ArgumentChecker.isTrue(_nExpiries == impliedVols.length, "implied volatilities wrong length; have {}, need {}", impliedVols.length, _nExpiries);
    for (int i = 0; i < strikes.length; i++) {
      ArgumentChecker.isTrue(strikes[i].length == impliedVols[i].length,
          "implied volatilities for expiry {} not the same length as strikes; have {}, need {}", strikes[i].length, impliedVols[i].length);
    }
    checkVolatilities(expiries, impliedVols);
    _expiries = expiries;
    _forwards = forwards;
    _forwardCurve = new ForwardCurve(InterpolatedDoublesCurve.from(_expiries, _forwards, forwardCurveInterpolator));
    _strikes = strikes;
    _impliedVols = impliedVols;
    _isCallData = isCallData;
  }

  public StandardSmileSurfaceDataBundle(final ForwardCurve forwardCurve, final double[] expiries, final double[][] strikes, final double[][] impliedVols, final boolean isCallData) {
    ArgumentChecker.notNull(forwardCurve, "forward curve");
    ArgumentChecker.notNull(expiries, "expiries");
    ArgumentChecker.notNull(strikes, "strikes");
    ArgumentChecker.notNull(impliedVols, "implied volatilities");
    _nExpiries = expiries.length;
    ArgumentChecker.isTrue(_nExpiries == strikes.length, "strikes wrong length; have {}, need {}", _nExpiries, strikes.length);
    ArgumentChecker.isTrue(_nExpiries == impliedVols.length, "implied volatilities wrong length; have {}, need {}", _nExpiries, impliedVols.length);
    _forwards = new double[_nExpiries];
    for (int i = 0; i < _nExpiries; i++) {
      ArgumentChecker.isTrue(strikes[i].length == impliedVols[i].length,
          "implied volatilities for expiry {} not the same length as strikes; have {}, need {}", strikes[i].length, impliedVols[i].length);
      _forwards[i] = forwardCurve.getForward(expiries[i]);
    }
    checkVolatilities(expiries, impliedVols);
    _expiries = expiries;
    _strikes = strikes;
    _impliedVols = impliedVols;
    _isCallData = isCallData;
    _forwardCurve = forwardCurve;
  }

  @Override
  public double[] getExpiries() {
    return _expiries;
  }

  @Override
  public double[][] getStrikes() {
    return _strikes;
  }

  @Override
  public double[][] getVolatilities() {
    return _impliedVols;
  }

  @Override
  public double[] getForwards() {
    return _forwards;
  }

  @Override
  public ForwardCurve getForwardCurve() {
    return _forwardCurve;
  }

  @Override
  public boolean isCallData() {
    return _isCallData;
  }

  @Override
  public SmileSurfaceDataBundle withBumpedPoint(final int expiryIndex, final int strikeIndex, final double amount) {
    ArgumentChecker.isTrue(ArgumentChecker.isInRangeExcludingHigh(0, _nExpiries, expiryIndex), "Invalid index for expiry; {}", expiryIndex);
    final double[][] strikes = getStrikes();
    ArgumentChecker.isTrue(ArgumentChecker.isInRangeExcludingHigh(0, strikes[expiryIndex].length, strikeIndex), "Invalid index for strike; {}", strikeIndex);
    final int nStrikes = strikes[expiryIndex].length;
    final double[][] vols = new double[_nExpiries][];
    for (int i = 0; i < _nExpiries; i++) {
      vols[i] = new double[nStrikes];
      System.arraycopy(_impliedVols[i], 0, vols[i], 0, nStrikes);
    }
    vols[expiryIndex][strikeIndex] += amount;
    return new StandardSmileSurfaceDataBundle(getForwardCurve(), getExpiries(), getStrikes(), vols, isCallData());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _forwardCurve.hashCode();
    result = prime * result + Arrays.deepHashCode(_impliedVols);
    result = prime * result + (_isCallData ? 1231 : 1237);
    result = prime * result + Arrays.deepHashCode(_strikes);
    result = prime * result + Arrays.hashCode(_expiries);
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
    final StandardSmileSurfaceDataBundle other = (StandardSmileSurfaceDataBundle) obj;
    if (!ObjectUtils.equals(_forwardCurve, other._forwardCurve)) {
      return false;
    }
    if (!Arrays.equals(_expiries, other._expiries)) {
      return false;
    }
    for (int i = 0; i < _nExpiries; i++) {
      if (!Arrays.equals(_strikes[i], other._strikes[i])) {
        return false;
      }
      if (!Arrays.equals(_impliedVols[i], other._impliedVols[i])) {
        return false;
      }
    }
    if (_isCallData != other._isCallData) {
      return false;
    }
    return true;
  }
}
