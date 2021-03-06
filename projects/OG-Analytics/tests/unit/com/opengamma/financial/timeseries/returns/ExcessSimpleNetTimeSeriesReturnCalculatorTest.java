/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.returns;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.util.timeseries.TimeSeriesException;
import com.opengamma.util.timeseries.localdate.ArrayLocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;

/**
 * 
 */
public class ExcessSimpleNetTimeSeriesReturnCalculatorTest {
  private static final TimeSeriesReturnCalculator CALCULATOR = TimeSeriesReturnCalculatorFactory.getReturnCalculator(TimeSeriesReturnCalculatorFactory.EXCESS_SIMPLE_NET_STRICT);
  private static final LocalDateDoubleTimeSeries TS1 = new ArrayLocalDateDoubleTimeSeries(new LocalDate[] {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2), LocalDate.of(2000, 1, 3), 
                                                                                                           LocalDate.of(2000, 1, 4), LocalDate.of(2000, 1, 5) }, 
                                                                                          new double[] {1, 2, 3, 4, 5});
  private static final LocalDateDoubleTimeSeries TS2 = new ArrayLocalDateDoubleTimeSeries(new LocalDate[] {LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 2), LocalDate.of(2000, 1, 3), 
                                                                                                           LocalDate.of(2000, 1, 4), LocalDate.of(2000, 1, 5) }, 
                                                                                          new double[] {1, 1, 1, 1, 1});
  private static final LocalDateDoubleTimeSeries EMPTY_SERIES = new ArrayLocalDateDoubleTimeSeries();
  private static final TimeSeriesReturnCalculator RETURNS = TimeSeriesReturnCalculatorFactory.getReturnCalculator(TimeSeriesReturnCalculatorFactory.SIMPLE_NET_STRICT);
  

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    CALCULATOR.evaluate((LocalDateDoubleTimeSeries[]) null);
  }

  @Test(expectedExceptions = TimeSeriesException.class)
  public void testSmallArray() {
    CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {TS1, TS2 });
  }

  @Test(expectedExceptions = TimeSeriesException.class)
  public void testDifferentLengths() {
    CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {TS1, null, new ArrayLocalDateDoubleTimeSeries(new LocalDate[] {LocalDate.of(2000, 1, 1)}, new double[] {1}), null});
  }

  @Test
  public void test() {
    assertEquals(CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {TS1, EMPTY_SERIES, TS2, EMPTY_SERIES}), RETURNS
        .evaluate(TS1));
    assertEquals(CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {TS1, null, TS2, null}), RETURNS.evaluate(TS1));
  }
}
