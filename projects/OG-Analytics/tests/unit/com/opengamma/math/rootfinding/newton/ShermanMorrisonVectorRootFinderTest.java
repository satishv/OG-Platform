/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.math.rootfinding.newton;

import org.testng.annotations.Test;

import com.opengamma.math.linearalgebra.SVDecompositionCommons;

/**
 * 
 */
public class ShermanMorrisonVectorRootFinderTest extends VectorRootFinderTest {
  private static final NewtonVectorRootFinder DEFAULT = new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  private static final NewtonVectorRootFinder SV = new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, new SVDecompositionCommons());
  private static final NewtonVectorRootFinder DEFAULT_JACOBIAN_2D = new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  // private static final NewtonVectorRootFinder SV_JACOBIAN_2D =
  // new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, JACOBIAN2D_CALCULATOR, new SVDecompositionCommons());
  private static final NewtonVectorRootFinder DEFAULT_JACOBIAN_3D = new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS);
  private static final NewtonVectorRootFinder SV_JACOBIAN_3D = new ShermanMorrisonVectorRootFinder(TOLERANCE, TOLERANCE, MAXSTEPS, new SVDecompositionCommons());

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSingular1() {
    assertFunction2D(DEFAULT, EPS);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSingular2() {
    assertFunction2D(DEFAULT_JACOBIAN_2D, EPS);
  }

  @Test
  public void test() {
    assertLinear(DEFAULT, EPS);
    assertLinear(SV, EPS);
    assertFunction2D(SV, EPS);
    // testFunction2D(SV_JACOBIAN_2D, EPS);
    assertFunction3D(DEFAULT, EPS);
    assertFunction3D(DEFAULT_JACOBIAN_3D, EPS);
    assertFunction3D(SV, EPS);
    assertFunction3D(SV_JACOBIAN_3D, EPS);
    assertYieldCurveBootstrap(DEFAULT, EPS);
  }
}
