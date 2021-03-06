/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.timeseries.returns;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import javax.time.calendar.LocalDate;

import org.testng.Assert;
import org.testng.annotations.Test;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.MersenneTwister64;
import cern.jet.random.engine.RandomEngine;

import com.opengamma.math.function.Function;
import com.opengamma.util.CalculationMode;
import com.opengamma.util.timeseries.TimeSeriesException;
import com.opengamma.util.timeseries.localdate.ArrayLocalDateDoubleTimeSeries;
import com.opengamma.util.timeseries.localdate.LocalDateDoubleTimeSeries;

/**
 * 
 */

public class SimpleNetTimeSeriesReturnCalculatorTest {
  private static final RandomEngine RANDOM = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);
  private static final Function<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> CALCULATOR = new SimpleNetTimeSeriesReturnCalculator(CalculationMode.LENIENT);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    CALCULATOR.evaluate((LocalDateDoubleTimeSeries[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyArray() {
    CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[0]);
  }

  @Test(expectedExceptions = TimeSeriesException.class)
  public void testWithShortTS() {
    final LocalDateDoubleTimeSeries ts = new ArrayLocalDateDoubleTimeSeries(new LocalDate[] {LocalDate.ofEpochDays(14022000)}, new double[] {4});
    CALCULATOR.evaluate(ts);
  }

  @Test
  public void testReturnsWithoutDividends() {
    final int n = 20;
    final LocalDate[] times = new LocalDate[n];
    final double[] data = new double[n];
    final double[] returns = new double[n - 1];
    double random;
    for (int i = 0; i < n; i++) {
      times[i] = LocalDate.ofEpochDays(i);
      random = RANDOM.nextDouble();
      data[i] = random;
      if (i > 0) {
        returns[i - 1] = random / data[i - 1] - 1;
      }
    }
    final LocalDateDoubleTimeSeries priceTS = new ArrayLocalDateDoubleTimeSeries(times, data);
    final LocalDateDoubleTimeSeries returnTS = new ArrayLocalDateDoubleTimeSeries(Arrays.copyOfRange(times, 1, n), returns);
    assertTrue(CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {priceTS}).equals(returnTS));
  }

  @Test
  public void testReturnsWithZeroesInSeries() {
    final int n = 20;
    final LocalDate[] times = new LocalDate[n];
    final double[] data = new double[n];
    final double[] returns = new double[n - 3];
    double random;
    for (int i = 0; i < n - 2; i++) {
      times[i] = LocalDate.ofEpochDays(i);
      random = RANDOM.nextDouble();
      data[i] = random;
      if (i > 0) {
        returns[i - 1] = random / data[i - 1] - 1;
      }
    }
    times[n - 2] = LocalDate.ofEpochDays(n - 2);
    data[n - 2] = 0;
    times[n - 1] = LocalDate.ofEpochDays(n - 1);
    data[n - 1] = RANDOM.nextDouble();
    final LocalDateDoubleTimeSeries priceTS = new ArrayLocalDateDoubleTimeSeries(times, data);
    final LocalDateDoubleTimeSeries returnTS = new ArrayLocalDateDoubleTimeSeries(Arrays.copyOfRange(times, 1, n - 2), returns);
    final TimeSeriesReturnCalculator strict = new SimpleNetTimeSeriesReturnCalculator(CalculationMode.STRICT);
    final LocalDateDoubleTimeSeries[] tsArray = new LocalDateDoubleTimeSeries[] {priceTS};
    try {
      strict.evaluate(tsArray);
      Assert.fail();
    } catch (final TimeSeriesException e) {
      // Expected
    }
    final TimeSeriesReturnCalculator lenient = new SimpleNetTimeSeriesReturnCalculator(CalculationMode.LENIENT);
    assertTrue(lenient.evaluate(tsArray).equals(returnTS));
  }

  @Test
  public void testReturnsWithDividendsAtDifferentTimes() {
    final int n = 20;
    final LocalDate[] times = new LocalDate[n];
    final double[] data = new double[n];
    final double[] returns = new double[n - 1];
    double random;
    for (int i = 0; i < n; i++) {
      times[i] = LocalDate.ofEpochDays(i);
      random = RANDOM.nextDouble();
      data[i] = random;
      if (i > 0) {
        returns[i - 1] = random / data[i - 1] - 1;
      }
    }
    final LocalDateDoubleTimeSeries dividendTS = new ArrayLocalDateDoubleTimeSeries(new LocalDate[] {LocalDate.ofEpochDays(300) }, new double[] {3});
    final LocalDateDoubleTimeSeries priceTS = new ArrayLocalDateDoubleTimeSeries(times, data);
    final LocalDateDoubleTimeSeries returnTS = new ArrayLocalDateDoubleTimeSeries(Arrays.copyOfRange(times, 1, n), returns);
    assertTrue(CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {priceTS, dividendTS}).equals(returnTS));
  }

  @Test
  public void testReturnsWithDividend() {
    final int n = 20;
    final LocalDate[] times = new LocalDate[n];
    final double[] data = new double[n];
    final double[] returns = new double[n - 1];
    final LocalDate[] dividendTimes = new LocalDate[] { LocalDate.ofEpochDays(1), LocalDate.ofEpochDays(4) };
    final double[] dividendData = new double[] {0.4, 0.6};
    double random;
    for (int i = 0; i < n; i++) {
      times[i] = LocalDate.ofEpochDays(i);
      random = RANDOM.nextDouble();
      data[i] = random;
      if (i > 0) {
        if (i == 1) {
          returns[i - 1] = (random + dividendData[0]) / data[i - 1] - 1;
        } else if (i == 4) {
          returns[i - 1] = (random + dividendData[1]) / data[i - 1] - 1;
        } else {
          returns[i - 1] = random / data[i - 1] - 1;
        }
      }
    }
    final LocalDateDoubleTimeSeries dividendTS = new ArrayLocalDateDoubleTimeSeries(dividendTimes, dividendData);
    final LocalDateDoubleTimeSeries priceTS = new ArrayLocalDateDoubleTimeSeries(times, data);
    final LocalDateDoubleTimeSeries returnTS = new ArrayLocalDateDoubleTimeSeries(Arrays.copyOfRange(times, 1, n), returns);
    assertTrue(CALCULATOR.evaluate(new LocalDateDoubleTimeSeries[] {priceTS, dividendTS}).equals(returnTS));
  }
}
