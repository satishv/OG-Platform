/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.math.rootfinding.newton;

import org.testng.annotations.Test;

import com.opengamma.math.function.Function1D;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.matrix.DoubleMatrix2D;

/**
 * 
 */
public class NewtonDefaultUpdateFunctionTest {
  private static final NewtonDefaultUpdateFunction F = new NewtonDefaultUpdateFunction();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    F.getUpdatedMatrix(null, null, null, null, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVector() {
    F.getUpdatedMatrix(new Function1D<DoubleMatrix1D, DoubleMatrix2D>() {

      @Override
      public DoubleMatrix2D evaluate(DoubleMatrix1D x) {
        return null;
      }
    }, null, null, null, null);
  }

}
