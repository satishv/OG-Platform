/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.web.server.conversion;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.model.volatility.surface.BlackVolatilitySurfaceMoneyness;
import com.opengamma.math.MathException;
import com.opengamma.math.surface.FunctionalDoublesSurface;
import com.opengamma.math.surface.InterpolatedDoublesSurface;

/**
 * 
 */
public class BlackVolatilitySurfaceMoneynessConverter implements ResultConverter<BlackVolatilitySurfaceMoneyness> {
  private static final DecimalFormat LABEL_FORMAT = new DecimalFormat("##.##");
  
  @Override
  public Object convertForDisplay(ResultConverterCache context, ValueSpecification valueSpec, BlackVolatilitySurfaceMoneyness value, ConversionMode mode) {
    Map<String, Object> result = new HashMap<String, Object>();
    if (value.getSurface() instanceof InterpolatedDoublesSurface) {
      InterpolatedDoublesSurface interpolated = (InterpolatedDoublesSurface) value.getSurface();
      result.put("xCount", interpolated.getXData().length);
      result.put("yCount", interpolated.getYData().length);
      if (mode == ConversionMode.FULL) {
        Double[] xs = interpolated.getXData();
        Double[] ys = interpolated.getYData();
        List<Double> uniqueX = new ArrayList<Double>();
        List<Double> uniqueY = new ArrayList<Double>();
        for (Double x : xs) {
          if (!uniqueX.contains(x)) {
            uniqueX.add(x);
          }
        }
        for (Double y : ys) {
          if (!uniqueY.contains(y)) {
            uniqueY.add(y);
          }
        }
        Collections.sort(uniqueX);
        Collections.sort(uniqueY);      
        Object[] xLabels = new Object[uniqueX.size()];
        Object[] yLabels = new Object[uniqueY.size()];
        double[][] surface = new double[xs.length][ys.length];
        boolean[][] missingValues = new boolean[xs.length][ys.length];
        for (int i = 0; i < uniqueX.size(); i++) {
          xLabels[i] = LABEL_FORMAT.format(uniqueX.get(i));
          for (int j = 0; j < uniqueY.size(); j++) {
            if (i == 0) {
              yLabels[j] = LABEL_FORMAT.format(uniqueY.get(j));
            }
            try {
              surface[i][j] = interpolated.getZValue(uniqueX.get(i), uniqueY.get(i));
            } catch (MathException e) {
              surface[i][j] = Double.MAX_VALUE;
              missingValues[i][j] = true;
            }
          }
        }
        result.put("xs", xLabels);
        result.put("ys", yLabels);
        result.put("surface", surface);
        result.put("missingValues", missingValues);
      }
    } else if (value.getSurface() instanceof FunctionalDoublesSurface) {
      FunctionalDoublesSurface functional = (FunctionalDoublesSurface) value.getSurface();
      result.put("xCount", 20);
      result.put("yCount", 20);
      if (mode == ConversionMode.FULL) {
        String[] xLabels = new String[20];
        String[] yLabels = new String[20];
        double[][] surface = new double[20][20];
        boolean[][] missingValues = new boolean[20][20];
        double x = 0;
        for (int i = 0; i < 20; i++) {
          x += 0.5;
          xLabels[i] = LABEL_FORMAT.format(x);
          double y = .5;
          for (int j = 0; j < 20; j++) {
            y += 0.05;
            if (i == 0) {
              yLabels[j] = LABEL_FORMAT.format(y);
            }
            surface[i][j] = 100 * functional.getZValue(x, y);
          }
        }
        result.put("xs", xLabels);
        result.put("ys", yLabels);
        result.put("surface", surface);
        result.put("missingValues", missingValues);
      }
    }
    return result;
  }

  @Override
  public Object convertForHistory(ResultConverterCache context, ValueSpecification valueSpec, BlackVolatilitySurfaceMoneyness value) {
    return null;
  }

  @Override
  public String convertToText(ResultConverterCache context, ValueSpecification valueSpec, BlackVolatilitySurfaceMoneyness value) {
    return "Black Volatility Surface Moneyness";
  }

  @Override
  public String getFormatterName() {
    return "SURFACE_DATA";
  }

}
