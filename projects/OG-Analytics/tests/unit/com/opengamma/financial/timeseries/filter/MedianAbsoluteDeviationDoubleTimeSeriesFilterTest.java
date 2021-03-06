/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.filter;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.LocalDate;

import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

import com.opengamma.util.timeseries.localdate.ArrayLocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;

/**
 * 
 */
public class MedianAbsoluteDeviationDoubleTimeSeriesFilterTest {
  private static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  private static final double LIMIT = 5;
  private static final double DATA1 = 29;
  private static final double DATA2 = 16;
  private static final TimeSeriesFilter FILTER = new MedianAbsoluteDeviationDoubleTimeSeriesFilter(LIMIT);
  private static final int N = 500;
  private static final LocalDate[] DATES = new LocalDate[N];
  private static final double[] DATA = new double[N];
  private static final LocalDateDoubleTimeSeries TS;
  private static final double EPS = 1e-15;
  private static final LocalDateDoubleTimeSeries EMPTY_SERIES = new ArrayLocalDateDoubleTimeSeries();

  static {
    for (int i = 0; i < N; i++) {
      DATES[i] = LocalDate.ofEpochDays(i);
      DATA[i] = RANDOM.nextDouble();
    }
    DATA[0] = DATA1;
    DATA[1] = DATA2;
    TS = new ArrayLocalDateDoubleTimeSeries(DATES, DATA);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    FILTER.evaluate((LocalDateDoubleTimeSeries) null);
  }

  @Test
  public void testEmptyTS() {
    final FilteredTimeSeries filtered = FILTER.evaluate(EMPTY_SERIES);
    assertEquals(filtered.getFilteredTS(), EMPTY_SERIES);
    assertEquals(filtered.getRejectedTS(), EMPTY_SERIES);
  }

  @Test
  public void testMasked() {
    final LocalDateDoubleTimeSeries subSeries = TS.subSeries(DATES[0], DATES[11]);
    final FilteredTimeSeries result = FILTER.evaluate(new ArrayLocalDateDoubleTimeSeries(subSeries));
    assertTimeSeries(result, 9);
  }

  @Test
  public void test() {
    assertTimeSeries(FILTER.evaluate(TS), 498);
  }

  private void assertTimeSeries(final FilteredTimeSeries result, final int size) {
    assertEquals(result.getFilteredTS().size(), size);
    final LocalDateDoubleTimeSeries rejected = result.getRejectedTS();
    assertEquals(rejected.getTimeAt(0), LocalDate.ofEpochDays(0));
    assertEquals(rejected.getValueAt(0), DATA1, EPS);
    assertEquals(rejected.getTimeAt(1), LocalDate.ofEpochDays(1));
    assertEquals(rejected.getValueAt(1), DATA2, EPS);
  }
}
