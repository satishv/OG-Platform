/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.option.definition;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.financial.model.volatility.surface.VolatilitySurface;
import com.opengamma.math.curve.ConstantDoublesCurve;
import com.opengamma.math.surface.ConstantDoublesSurface;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;
import com.opengamma.util.timeseries.DoubleTimeSeries;
import com.opengamma.util.timeseries.fast.DateTimeNumericEncoding;
import com.opengamma.util.timeseries.fast.integer.FastArrayIntDoubleTimeSeries;

/**
 * 
 */
public class FloatingStrikeLookbackOptionDefinitionTest {
  private static final ZonedDateTime DATE = DateUtils.getUTCDate(2010, 6, 1);
  private static final Expiry EXPIRY = new Expiry(DATE);
  private static final FloatingStrikeLookbackOptionDefinition CALL = new FloatingStrikeLookbackOptionDefinition(EXPIRY, true);
  private static final FloatingStrikeLookbackOptionDefinition PUT = new FloatingStrikeLookbackOptionDefinition(EXPIRY, false);
  private static final double SPOT = 100;
  private static final double DIFF = 10;
  private static final DoubleTimeSeries<?> HIGH_TS = new FastArrayIntDoubleTimeSeries(DateTimeNumericEncoding.DATE_DDMMYYYY, new int[] {20100529, 20100530, 20100531}, new double[] {SPOT, SPOT + DIFF,
      SPOT});
  private static final DoubleTimeSeries<?> LOW_TS = new FastArrayIntDoubleTimeSeries(DateTimeNumericEncoding.DATE_DDMMYYYY, new int[] {20100529, 20100530, 20100531}, new double[] {SPOT, SPOT - DIFF,
      SPOT});
  private static final StandardOptionWithSpotTimeSeriesDataBundle HIGH_DATA = new StandardOptionWithSpotTimeSeriesDataBundle(new YieldCurve(ConstantDoublesCurve.from(0.1)), 0.05,
      new VolatilitySurface(ConstantDoublesSurface.from(0.2)), SPOT, DateUtils.getUTCDate(2010, 6, 1), HIGH_TS);
  private static final StandardOptionWithSpotTimeSeriesDataBundle LOW_DATA = new StandardOptionWithSpotTimeSeriesDataBundle(new YieldCurve(ConstantDoublesCurve.from(0.1)), 0.05,
      new VolatilitySurface(ConstantDoublesSurface.from(0.2)), SPOT, DateUtils.getUTCDate(2010, 6, 1), LOW_TS);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDataBundle() {
    CALL.getPayoffFunction().getPayoff(null, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullTS() {
    CALL.getPayoffFunction().getPayoff(HIGH_DATA.withSpotTimeSeries(null), null);
  }

  @Test
  public void testExercise() {
    assertFalse(CALL.getExerciseFunction().shouldExercise(HIGH_DATA, null));
    assertFalse(CALL.getExerciseFunction().shouldExercise(LOW_DATA, null));
    assertFalse(PUT.getExerciseFunction().shouldExercise(HIGH_DATA, null));
    assertFalse(PUT.getExerciseFunction().shouldExercise(LOW_DATA, null));
  }

  @Test
  public void testPayoff() {
    final double eps = 1e-15;
    OptionPayoffFunction<StandardOptionWithSpotTimeSeriesDataBundle> payoff = CALL.getPayoffFunction();
    assertEquals(payoff.getPayoff(LOW_DATA, 0.), DIFF, eps);
    assertEquals(payoff.getPayoff(HIGH_DATA, 0.), 0, eps);
    payoff = PUT.getPayoffFunction();
    assertEquals(payoff.getPayoff(LOW_DATA, 0.), 0, eps);
    assertEquals(payoff.getPayoff(HIGH_DATA, 0.), DIFF, eps);
  }

  @Test
  public void testEqualsAndHashCode() {
    final OptionDefinition call1 = new FloatingStrikeLookbackOptionDefinition(EXPIRY, true);
    final OptionDefinition put1 = new FloatingStrikeLookbackOptionDefinition(EXPIRY, false);
    final OptionDefinition call2 = new FloatingStrikeLookbackOptionDefinition(new Expiry(DateUtils.getDateOffsetWithYearFraction(DATE, 3)), true);
    final OptionDefinition put2 = new FloatingStrikeLookbackOptionDefinition(new Expiry(DateUtils.getDateOffsetWithYearFraction(DATE, 3)), false);
    assertFalse(CALL.equals(PUT));
    assertEquals(call1, CALL);
    assertEquals(put1, PUT);
    assertEquals(call1.hashCode(), CALL.hashCode());
    assertEquals(put1.hashCode(), PUT.hashCode());
    assertFalse(call2.equals(CALL));
    assertFalse(put2.equals(PUT));
  }
}
