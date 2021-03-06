/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.volatility.local;

import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_INTERPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_LAMBDA;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_SURFACE_TYPE;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_X_AXIS;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_Y_AXIS;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.TreeSet;

import javax.time.calendar.Period;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.marketdatasnapshot.VolatilitySurfaceData;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.financial.analytics.model.forex.ForexVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.volatility.surface.BloombergFXOptionVolatilitySurfaceInstrumentProvider.FXVolQuoteType;
import com.opengamma.financial.analytics.volatility.surface.RawVolatilitySurfaceDataFunction;
import com.opengamma.financial.model.interestrate.curve.ForwardCurve;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.ForexSmileDeltaSurfaceDataBundle;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.SmileSurfaceDataBundle;
import com.opengamma.util.time.Tenor;
import com.opengamma.util.tuple.ObjectsPair;
import com.opengamma.util.tuple.Pair;

/**
 * 
 */
public class ForexPiecewiseSABRSurfaceFunction extends PiecewiseSABRSurfaceFunction {
  private static final Logger s_logger = LoggerFactory.getLogger(ForexPiecewiseSABRSurfaceFunction.class);

  public ForexPiecewiseSABRSurfaceFunction() {
    super(ForexVolatilitySurfaceFunction.INSTRUMENT_TYPE);
  }

  @Override
  protected ValueProperties getResultProperties() {
    return createValueProperties()
        .withAny(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE)
        .withAny(ValuePropertyNames.SURFACE)
        .withAny(PROPERTY_SURFACE_TYPE)
        .withAny(PROPERTY_X_AXIS)
        .withAny(PROPERTY_Y_AXIS)
        .withAny(PROPERTY_LAMBDA)
        .withAny(LocalVolatilityPDEValuePropertyNames.PROPERTY_H)
        .withAny(ValuePropertyNames.CURVE_CALCULATION_METHOD)
        .withAny(PROPERTY_FORWARD_CURVE_INTERPOLATOR)
        .withAny(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR)
        .withAny(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR).get();
  }

  @Override
  protected ValueProperties getResultProperties(final String surfaceName, final String forwardCurveCalculationMethod, final String forwardCurveInterpolator,
      final String forwardCurveLeftExtrapolator, final String forwardCurveRightExtrapolator) {
    return createValueProperties()
        .with(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE, ForexVolatilitySurfaceFunction.INSTRUMENT_TYPE)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .withAny(PROPERTY_SURFACE_TYPE)
        .withAny(PROPERTY_X_AXIS)
        .withAny(PROPERTY_Y_AXIS)
        .withAny(PROPERTY_LAMBDA)
        .with(ValuePropertyNames.CURVE_CALCULATION_METHOD, forwardCurveCalculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator).get();
  }

  @Override
  protected ValueProperties getResultProperties(final String surfaceName, final String surfaceType, final String xAxis, final String yAxis, final String lambda,
      final String forwardCurveCalculationMethod, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator) {
    return createValueProperties()
        .with(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE, ForexVolatilitySurfaceFunction.INSTRUMENT_TYPE)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(PROPERTY_SURFACE_TYPE, surfaceType)
        .with(PROPERTY_X_AXIS, xAxis)
        .with(PROPERTY_Y_AXIS, yAxis)
        .with(PROPERTY_LAMBDA, lambda)
        .with(ValuePropertyNames.CURVE_CALCULATION_METHOD, forwardCurveCalculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator).get();
  }

  @Override
  protected SmileSurfaceDataBundle getData(final FunctionInputs inputs, final ValueRequirement volDataRequirement, final ValueRequirement forwardCurveRequirement) {
    final Object volatilitySurfaceObject = inputs.getValue(volDataRequirement);
    if (volatilitySurfaceObject == null) {
      throw new OpenGammaRuntimeException("Could not get " + volDataRequirement);
    }
    final Object forwardCurveObject = inputs.getValue(forwardCurveRequirement);
    if (forwardCurveObject == null) {
      throw new OpenGammaRuntimeException("Could not get " + forwardCurveRequirement);
    }
    final ForwardCurve forwardCurve = (ForwardCurve) forwardCurveObject;
    @SuppressWarnings("unchecked")
    final VolatilitySurfaceData<Tenor, Pair<Number, FXVolQuoteType>> fxVolatilitySurface = (VolatilitySurfaceData<Tenor, Pair<Number, FXVolQuoteType>>) volatilitySurfaceObject;
    final Tenor[] tenors = fxVolatilitySurface.getXs();
    Arrays.sort(tenors);
    final Pair<Number, FXVolQuoteType>[] quotes = fxVolatilitySurface.getYs();
    final Number[] deltaValues = getDeltaValues(quotes);
    final int nExpiries = tenors.length;
    final int nDeltas = deltaValues.length - 1;
    final double[] expiries = new double[nExpiries];
    final double[] deltas = new double[nDeltas];
    final double[] atms = new double[nExpiries];
    final double[][] riskReversals = new double[nDeltas][nExpiries];
    final double[][] strangle = new double[nDeltas][nExpiries];
    for (int i = 0; i < nExpiries; i++) {
      final Tenor tenor = tenors[i];
      final double t = getTime(tenor);
      final Double atm = fxVolatilitySurface.getVolatility(tenor, ObjectsPair.of(deltaValues[0], FXVolQuoteType.ATM));
      if (atm == null) {
        throw new OpenGammaRuntimeException("Could not get ATM volatility data for surface");
      }
      expiries[i] = t;
      atms[i] = atm;
    }
    for (int i = 0; i < nDeltas; i++) {
      final Number delta = deltaValues[i + 1];
      if (delta != null) {
        deltas[i] = delta.doubleValue() / 100.;
        final DoubleArrayList riskReversalList = new DoubleArrayList();
        final DoubleArrayList strangleList = new DoubleArrayList();
        for (int j = 0; j < nExpiries; j++) {
          final Double rr = fxVolatilitySurface.getVolatility(tenors[j], ObjectsPair.of(delta, FXVolQuoteType.RISK_REVERSAL));
          final Double s = fxVolatilitySurface.getVolatility(tenors[j], ObjectsPair.of(delta, FXVolQuoteType.BUTTERFLY));
          if (rr != null && s != null) {
            riskReversalList.add(rr);
            strangleList.add(s);
          } else {
            s_logger.info("Had a null value for tenor number " + j);
          }
        }
        riskReversals[i] = riskReversalList.toDoubleArray();
        strangle[i] = strangleList.toDoubleArray();
      }
    }
    final boolean isCallData = true; //TODO this shouldn't be hard-coded
    return new ForexSmileDeltaSurfaceDataBundle(forwardCurve, expiries, deltas, atms, riskReversals, strangle, isCallData);
  }

  private double getTime(final Tenor tenor) {
    final Period period = tenor.getPeriod();
    if (period.getYears() != 0) {
      return period.getYears();
    }
    if (period.getMonths() != 0) {
      return ((double) period.getMonths()) / 12;
    }
    if (period.getDays() != 0) {
      return ((double) period.getDays()) / 365;
    }
    throw new OpenGammaRuntimeException("Should never happen");
  }

  private Number[] getDeltaValues(final Pair<Number, FXVolQuoteType>[] quotes) {
    final TreeSet<Number> values = new TreeSet<Number>();
    for (final Pair<Number, FXVolQuoteType> pair : quotes) {
      values.add(pair.getFirst());
    }
    return values.toArray((Number[]) Array.newInstance(Number.class, values.size()));
  }
}
