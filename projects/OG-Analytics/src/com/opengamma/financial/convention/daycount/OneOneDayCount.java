/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.convention.daycount;

import javax.time.calendar.ZonedDateTime;

/**
 * The 1/1 day count convention.
 * <p>
 * The 1/1 day count always returns one as the fraction of a year.
 */
public class OneOneDayCount extends StatelessDayCount {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  @Override
  public double getDayCountFraction(final ZonedDateTime firstDate, final ZonedDateTime secondDate) {
    return 1;
  }

  @Override
  public double getAccruedInterest(final ZonedDateTime previousCouponDate, final ZonedDateTime date, final ZonedDateTime nextCouponDate, final double coupon, final double paymentsPerYear) {
    return coupon / paymentsPerYear;
  }

  @Override
  public String getConventionName() {
    return "1/1";
  }

}
