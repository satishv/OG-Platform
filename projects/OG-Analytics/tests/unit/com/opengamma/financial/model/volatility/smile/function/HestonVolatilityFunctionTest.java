/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.volatility.smile.function;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import com.opengamma.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.math.function.Function1D;

/**
 * 
 */
public class HestonVolatilityFunctionTest {

  private static final double KAPPA = 0.1;
  private static final double THETA = 0.05;
  private static final double VOL0 = 0.1;
  private static final double OMEGA = 0.7;
  private static final double RHO = -0.8;
  private static final HestonModelData DATA = new HestonModelData(KAPPA, THETA, VOL0, OMEGA, RHO);

  private static final double FORWARD = 0.012;
  private static final double TIME = 2.4;
  private static final double[] STRIKES;
  private static final VolatilityFunctionProvider<HestonModelData> VOL_FUNC_PROVIDER = new HestonVolatilityFunction();
  private static final List<Function1D<HestonModelData, Double>> VOL_FUNC_LIST;
  private static final Function1D<HestonModelData, double[]> VOL_FUNC_SET;

  private static final VolatilityFunctionProvider<HestonModelData> VOL_FUNC_PROVIDER_FD = new VolatilityFunctionProvider<HestonModelData>() {

    public Function1D<HestonModelData, Double> getVolatilityFunction(EuropeanVanillaOption option, double forward) {
      return VOL_FUNC_PROVIDER.getVolatilityFunction(option, forward);
    }
  };

  static {
    final int n = 11;
    VOL_FUNC_LIST = new ArrayList<Function1D<HestonModelData, Double>>(n);
    STRIKES = new double[n];
    for (int i = 0; i < n; i++) {
      double m = -1 + 2.0 * i / (n - 1);
      STRIKES[i] = FORWARD * Math.exp(m);
      EuropeanVanillaOption option = new EuropeanVanillaOption(STRIKES[i], TIME, false);
      VOL_FUNC_LIST.add(VOL_FUNC_PROVIDER.getVolatilityFunction(option, FORWARD));
    }
    VOL_FUNC_SET = VOL_FUNC_PROVIDER.getVolatilityFunction(FORWARD, STRIKES, TIME);
  }

  @Test
  public void testVolFunction() {
    final int n = STRIKES.length;
    double[] vols = VOL_FUNC_SET.evaluate(DATA);
    for (int i = 0; i < n; i++) {
      assertEquals(vols[i], VOL_FUNC_LIST.get(i).evaluate(DATA), 1e-4, "Strike: " + STRIKES[i]);
    }
  }

  @Test
  public void tesVolFunctionAdjoint() {
    final int n = STRIKES.length;
    Function1D<HestonModelData, double[][]> adjointSetFunc = VOL_FUNC_PROVIDER.getVolatilityAdjointFunction(FORWARD, STRIKES, TIME);
    Function1D<HestonModelData, double[][]> adjointSetFuncFD = VOL_FUNC_PROVIDER_FD.getVolatilityAdjointFunction(FORWARD, STRIKES, TIME);

    double[][] adjointSet = adjointSetFunc.evaluate(DATA);
    double[][] adjointSetFD = adjointSetFuncFD.evaluate(DATA);

    assertEquals(adjointSet.length, n, "#strikes");
    assertEquals(adjointSetFD.length, n, "#strikes FD");
    assertEquals(adjointSet[0].length, 8, "#parameters");
    assertEquals(adjointSetFD[0].length, 8, "#parameters FD");

    for (int i = 0; i < n; i++) {
      for (int pIndex = 3; pIndex < 8; pIndex++) {
        assertEquals(adjointSetFD[i][pIndex], adjointSet[i][pIndex], 5e-3, "parameter: " + pIndex + ". strike: " + i);
      }
    }

  }
}
