/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.math.curve;

import org.apache.commons.lang.Validate;

import com.opengamma.math.function.Function;

/**
 * A function that performs multiplication on each of the constituent curves.
 * <p> 
 * Given a number of curves {@latex.inline $C_1(x_{i_1}, y_{i_1}) , C_2(x_{i_2}, y_{i_2}), \\ldots C_n(x_{i_n}, y_{i_n})$}, returns a function {@latex.inline $F$}
 * that for a value {@latex.inline $x$} will return:
 * {@latex.ilb %preamble{\\usepackage{amsmath}} 
 * \\begin{eqnarray*}
 * F(x) = C_1 |_x \\times C_2 |_x \\times \\ldots \\times C_n |_x
 * \\end{eqnarray*}} 
 */

public class MultiplyCurveSpreadFunction implements CurveSpreadFunction {
  private static final String NAME = "*";

  /**
   * @param curves An array of curves, not null or empty
   * @return A function that will find the value of each curve at the given input <i>x</i> and multiply each in turn
   */
  @Override
  public Function<Double, Double> evaluate(final Curve<Double, Double>... curves) {
    Validate.notNull(curves, "x");
    Validate.notEmpty(curves, "curves");
    return new Function<Double, Double>() {

      @Override
      public Double evaluate(final Double... x) {
        Validate.notNull(x, "x");
        Validate.notEmpty(x, "x");
        final double x0 = x[0];
        double y = curves[0].getYValue(x0);
        for (int i = 1; i < curves.length; i++) {
          y *= curves[i].getYValue(x0);
        }
        return y;
      }

    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOperationName() {
    return NAME;
  }
}
