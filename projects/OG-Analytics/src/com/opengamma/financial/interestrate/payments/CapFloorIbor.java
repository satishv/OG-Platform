/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.payments;

/**
 * Class describing a cap/floor on Ibor.
 */
public class CapFloorIbor extends CouponIbor {

  /**
   * The cap/floor strike.
   */
  private final double _strike;
  /**
   * The cap (true) / floor (false) flag.
   */
  private final boolean _isCap;

  /**
   * Constructor from all the cap/floor details.
   * @param paymentTime Time (in years) up to the payment.
   * @param fundingCurveName Name of the funding curve.
   * @param paymentYearFraction The year fraction (or accrual factor) for the coupon payment.
   * @param notional Coupon notional.
   * @param fixingTime Time (in years) up to fixing.
   * @param fixingPeriodStartTime Time (in years) up to the start of the fixing period.
   * @param fixingPeriodEndTime Time (in years) up to the end of the fixing period.
   * @param fixingYearFraction The year fraction (or accrual factor) for the fixing period.
   * @param forwardCurveName Name of the forward (or estimation) curve.
   * @param strike The strike
   * @param isCap The cap/floor flag.
   */
  public CapFloorIbor(double paymentTime, String fundingCurveName, double paymentYearFraction, double notional, double fixingTime, double fixingPeriodStartTime, double fixingPeriodEndTime,
      double fixingYearFraction, String forwardCurveName, double strike, boolean isCap) {
    super(paymentTime, fundingCurveName, paymentYearFraction, notional, fixingTime, fixingPeriodStartTime, fixingPeriodEndTime, fixingYearFraction, forwardCurveName);
    _strike = strike;
    _isCap = isCap;
  }

  /**
   * Gets the _strike field.
   * @return The strike
   */
  public double getStrike() {
    return _strike;
  }

  /**
   * Gets the _isCap field.
   * @return The cap (true) / floor (false) flag.
   */
  public boolean isCap() {
    return _isCap;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (_isCap ? 1231 : 1237);
    long temp;
    temp = Double.doubleToLongBits(_strike);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CapFloorIbor other = (CapFloorIbor) obj;
    if (_isCap != other._isCap) {
      return false;
    }
    if (Double.doubleToLongBits(_strike) != Double.doubleToLongBits(other._strike)) {
      return false;
    }
    return true;
  }

}