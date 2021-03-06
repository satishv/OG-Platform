/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.volatility.local;

import static com.opengamma.engine.value.ValuePropertyNames.CURVE_CALCULATION_METHOD;
import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_INTERPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.InterpolatedForwardCurveValuePropertyNames.PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_H;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_LAMBDA;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_MAX_MONEYNESS;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_PDE_DIRECTION;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_SPACE_GRID_BUNCHING;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_SPACE_STEPS;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_SURFACE_TYPE;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_THETA;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_TIME_GRID_BUNCHING;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_TIME_STEPS;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_X_AXIS;
import static com.opengamma.financial.analytics.model.volatility.local.LocalVolatilityPDEValuePropertyNames.PROPERTY_Y_AXIS;
import static com.opengamma.financial.analytics.volatility.surface.RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.time.calendar.Clock;
import javax.time.calendar.ZonedDateTime;

import com.google.common.collect.Sets;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.analytics.volatility.surface.RawVolatilitySurfaceDataFunction;
import com.opengamma.financial.model.interestrate.curve.ForwardCurve;
import com.opengamma.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.financial.model.volatility.local.DupireLocalVolatilityCalculator;
import com.opengamma.financial.model.volatility.local.LocalVolatilityForwardPDEGreekCalculator;
import com.opengamma.financial.model.volatility.local.LocalVolatilitySurface;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.MoneynessPiecewiseSABRSurfaceFitter;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.PiecewiseSABRSurfaceFitter1;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.SmileSurfaceDataBundle;
import com.opengamma.financial.model.volatility.smile.fitting.sabr.StrikePiecewiseSABRSurfaceFitter;
import com.opengamma.financial.model.volatility.surface.Moneyness;
import com.opengamma.financial.model.volatility.surface.Strike;
import com.opengamma.financial.security.FinancialSecurity;
import com.opengamma.financial.security.option.FXOptionSecurity;
import com.opengamma.id.UniqueId;
import com.opengamma.util.money.UnorderedCurrencyPair;

/**
 * 
 */
public abstract class LocalVolatilityPDEGridFunction extends AbstractFunction.NonCompiledInvoker {
  private final String _instrumentType;

  public LocalVolatilityPDEGridFunction(final String instrumentType) {
    _instrumentType = instrumentType;
  }

  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target, final Set<ValueRequirement> desiredValues) {
    final Clock snapshotClock = executionContext.getValuationClock();
    final ZonedDateTime now = snapshotClock.zonedDateTime();
    final ValueRequirement desiredValue = desiredValues.iterator().next();
    final String surfaceName = desiredValue.getConstraint(ValuePropertyNames.SURFACE);
    final String surfaceType = desiredValue.getConstraint(PROPERTY_SURFACE_TYPE);
    final boolean moneynessSurface = LocalVolatilityPDEUtils.isMoneynessSurface(surfaceType);
    final String xAxis = desiredValue.getConstraint(PROPERTY_X_AXIS);
    final boolean useLogTime = LocalVolatilityPDEUtils.useLogTime(xAxis);
    final String yAxis = desiredValue.getConstraint(PROPERTY_Y_AXIS);
    final boolean useIntegratedVar = LocalVolatilityPDEUtils.useIntegratedVariance(yAxis);
    final String lambdaName = desiredValue.getConstraint(PROPERTY_LAMBDA);
    final double lambda = Double.parseDouble(lambdaName);
    final String forwardCurveCalculationMethod = desiredValue.getConstraint(CURVE_CALCULATION_METHOD);
    final String forwardCurveInterpolator = desiredValue.getConstraint(PROPERTY_FORWARD_CURVE_INTERPOLATOR);
    final String forwardCurveLeftExtrapolator = desiredValue.getConstraint(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR);
    final String forwardCurveRightExtrapolator = desiredValue.getConstraint(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR);
    final String hName = desiredValue.getConstraint(PROPERTY_H);
    final double h = Double.parseDouble(hName);
    final String thetaName = desiredValue.getConstraint(PROPERTY_THETA);
    final double theta = Double.parseDouble(thetaName);
    final String timeStepsName = desiredValue.getConstraint(PROPERTY_TIME_STEPS);
    final int timeSteps = Integer.parseInt(timeStepsName);
    final String spaceStepsName = desiredValue.getConstraint(PROPERTY_SPACE_STEPS);
    final int spaceSteps = Integer.parseInt(spaceStepsName);
    final String timeGridBunchingName = desiredValue.getConstraint(PROPERTY_TIME_GRID_BUNCHING);
    final double timeGridBunching = Double.parseDouble(timeGridBunchingName);
    final String spaceGridBunchingName = desiredValue.getConstraint(PROPERTY_SPACE_GRID_BUNCHING);
    final double spaceGridBunching = Double.parseDouble(spaceGridBunchingName);
    final String maxMoneynessName = desiredValue.getConstraint(PROPERTY_MAX_MONEYNESS);
    final double maxMoneyness  = Double.parseDouble(maxMoneynessName);
    final String pdeDirection = desiredValue.getConstraint(PROPERTY_PDE_DIRECTION);
    if (!(pdeDirection.equals(LocalVolatilityPDEValuePropertyNames.FORWARD_PDE))) {
      throw new OpenGammaRuntimeException("Can only use forward PDE; should never ask for this direction: " + pdeDirection);
    }
    PiecewiseSABRSurfaceFitter1<?> surfaceFitter;
    final DupireLocalVolatilityCalculator localVolatilityCalculator = new DupireLocalVolatilityCalculator(h);
    LocalVolatilityForwardPDEGreekCalculator<?> calculator;
    if (moneynessSurface) {
      surfaceFitter = new MoneynessPiecewiseSABRSurfaceFitter(useLogTime, useIntegratedVar, lambda);
      calculator = new LocalVolatilityForwardPDEGreekCalculator<Moneyness>(theta, timeSteps, spaceSteps, timeGridBunching, spaceGridBunching,
          (MoneynessPiecewiseSABRSurfaceFitter) surfaceFitter, localVolatilityCalculator, maxMoneyness);
    } else {
      surfaceFitter = new StrikePiecewiseSABRSurfaceFitter(useLogTime, useIntegratedVar, lambda);
      calculator = new LocalVolatilityForwardPDEGreekCalculator<Strike>(theta, timeSteps, spaceSteps, timeGridBunching, spaceGridBunching,
          (StrikePiecewiseSABRSurfaceFitter) surfaceFitter, localVolatilityCalculator, maxMoneyness);
    }
    final ValueSpecification spec = getResultSpec(target, surfaceName, surfaceType, xAxis, yAxis, lambdaName, forwardCurveCalculationMethod,
        hName, forwardCurveInterpolator, forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator, thetaName, timeStepsName, spaceStepsName,
        timeGridBunchingName, spaceGridBunchingName, maxMoneynessName, pdeDirection);
    final FXOptionSecurity security = (FXOptionSecurity) target.getSecurity();
    final UnorderedCurrencyPair currencies = UnorderedCurrencyPair.of(security.getCallCurrency(), security.getPutCurrency()); //TODO to subclass
    final ValueProperties surfaceProperties = getLocalVolSurfaceProperties(surfaceName, surfaceType, xAxis, yAxis, lambdaName, hName,
        forwardCurveCalculationMethod, forwardCurveInterpolator, forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator);
    final ValueRequirement surfaceRequirement = new ValueRequirement(ValueRequirementNames.LOCAL_VOLATILITY_SURFACE, ComputationTargetType.PRIMITIVE, currencies.getUniqueId(), surfaceProperties);
    final Object localVolatilitySurfaceObject = inputs.getValue(surfaceRequirement);
    if (localVolatilitySurfaceObject == null) {
      throw new OpenGammaRuntimeException("Local volatility surface was null");
    }
    final LocalVolatilitySurface<?> localVolatilitySurface = (LocalVolatilitySurface<?>) localVolatilitySurfaceObject;
    final ValueRequirement forwardCurveRequirement = getForwardCurveRequirement(forwardCurveCalculationMethod, forwardCurveInterpolator,
        forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator, currencies.getUniqueId());
    final Object forwardCurveObject = inputs.getValue(forwardCurveRequirement);
    if (forwardCurveObject == null) {
      throw new OpenGammaRuntimeException("Forward curve was null");
    }
    final ForwardCurve forwardCurve = (ForwardCurve) forwardCurveObject;
    final ValueRequirement volDataRequirement = new ValueRequirement(ValueRequirementNames.VOLATILITY_SURFACE_DATA, ComputationTargetType.PRIMITIVE,
        currencies.getUniqueId(),
        ValueProperties
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(PROPERTY_SURFACE_INSTRUMENT_TYPE, _instrumentType).get());
    final SmileSurfaceDataBundle data = getData(inputs, volDataRequirement, forwardCurveRequirement);
    final EuropeanVanillaOption option = getOption(security, now);
    return Collections.singleton(new ComputedValue(spec, getResult(calculator, localVolatilitySurface, forwardCurve, data, option)));
  }

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.SECURITY;
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
    final ValueProperties properties = createValueProperties()
        .with(ValuePropertyNames.CALCULATION_METHOD, LocalVolatilityPDEValuePropertyNames.LOCAL_VOLATILITY_METHOD)
        .withAny(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE)
        .withAny(ValuePropertyNames.SURFACE)
        .withAny(PROPERTY_SURFACE_TYPE)
        .withAny(PROPERTY_X_AXIS)
        .withAny(PROPERTY_Y_AXIS)
        .withAny(PROPERTY_LAMBDA)
        .withAny(CURVE_CALCULATION_METHOD)
        .withAny(PROPERTY_FORWARD_CURVE_INTERPOLATOR)
        .withAny(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR)
        .withAny(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR)
        .withAny(PROPERTY_THETA)
        .withAny(PROPERTY_TIME_STEPS)
        .withAny(PROPERTY_SPACE_STEPS)
        .withAny(PROPERTY_TIME_GRID_BUNCHING)
        .withAny(PROPERTY_SPACE_GRID_BUNCHING)
        .withAny(PROPERTY_MAX_MONEYNESS)
        .withAny(PROPERTY_H)
        .withAny(PROPERTY_PDE_DIRECTION).get();
    return Collections.singleton(new ValueSpecification(getResultName(), target.toSpecification(), properties));
  }

  @Override
  public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue) {
    final ValueProperties constraints = desiredValue.getConstraints();
    final Set<String> surfaceNames = constraints.getValues(ValuePropertyNames.SURFACE);
    if (surfaceNames == null || surfaceNames.size() != 1) {
      return null;
    }
    final Set<String> surfaceTypeNames = constraints.getValues(PROPERTY_SURFACE_TYPE);
    if (surfaceTypeNames == null || surfaceTypeNames.size() != 1) {
      return null;
    }
    final Set<String> xAxisNames = constraints.getValues(PROPERTY_X_AXIS);
    if (xAxisNames == null || xAxisNames.size() != 1) {
      return null;
    }
    final Set<String> yAxisNames = constraints.getValues(PROPERTY_Y_AXIS);
    if (yAxisNames == null || yAxisNames.size() != 1) {
      return null;
    }
    final Set<String> lambdaNames = constraints.getValues(PROPERTY_LAMBDA);
    if (lambdaNames == null || lambdaNames.size() != 1) {
      return null;
    }
    final Set<String> forwardCurveCalculationMethodNames = constraints.getValues(CURVE_CALCULATION_METHOD);
    if (forwardCurveCalculationMethodNames == null || forwardCurveCalculationMethodNames.size() != 1) {
      return null;
    }
    final Set<String> hNames = constraints.getValues(PROPERTY_H);
    if (hNames == null || hNames.size() != 1) {
      return null;
    }
    final Set<String> forwardCurveInterpolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_INTERPOLATOR);
    if (forwardCurveInterpolatorNames == null || forwardCurveInterpolatorNames.size() != 1) {
      return null;
    }
    final Set<String> forwardCurveLeftExtrapolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR);
    if (forwardCurveLeftExtrapolatorNames == null || forwardCurveLeftExtrapolatorNames.size() != 1) {
      return null;
    }
    final Set<String> forwardCurveRightExtrapolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR);
    if (forwardCurveRightExtrapolatorNames == null || forwardCurveRightExtrapolatorNames.size() != 1) {
      return null;
    }
    final String surfaceType = surfaceTypeNames.iterator().next();
    final String xAxis = xAxisNames.iterator().next();
    final String yAxis = yAxisNames.iterator().next();
    final String lambda = lambdaNames.iterator().next();
    final String forwardCurveCalculationMethod = forwardCurveCalculationMethodNames.iterator().next();
    final String h = hNames.iterator().next();
    final String forwardCurveInterpolator = forwardCurveInterpolatorNames.iterator().next();
    final String forwardCurveLeftExtrapolator = forwardCurveLeftExtrapolatorNames.iterator().next();
    final String forwardCurveRightExtrapolator = forwardCurveRightExtrapolatorNames.iterator().next();
    final String surfaceName = surfaceNames.iterator().next();
    final FXOptionSecurity fxOption = (FXOptionSecurity) target.getSecurity();
    final UniqueId pairUID = UnorderedCurrencyPair.of(fxOption.getCallCurrency(), fxOption.getPutCurrency()).getUniqueId(); //TODO down to subclass
    final ValueRequirement volDataRequirement = new ValueRequirement(ValueRequirementNames.VOLATILITY_SURFACE_DATA, ComputationTargetType.PRIMITIVE,
        pairUID,
        ValueProperties
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(PROPERTY_SURFACE_INSTRUMENT_TYPE, _instrumentType).get());
    return Sets.newHashSet(getVolatilitySurfaceRequirement(surfaceName, surfaceType, xAxis, yAxis, lambda, h,
        forwardCurveCalculationMethod, forwardCurveInterpolator, forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator, pairUID),
        getForwardCurveRequirement(forwardCurveCalculationMethod, forwardCurveInterpolator, forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator, pairUID),
        volDataRequirement);
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target, final Map<ValueSpecification, ValueRequirement> inputs) {
    String surfaceName = null;
    String surfaceType = null;
    String xAxis = null;
    String yAxis = null;
    String lambda = null;
    String forwardCurveCalculationMethod = null;
    String forwardCurveInterpolator = null;
    String forwardCurveLeftExtrapolator = null;
    String forwardCurveRightExtrapolator = null;
    String h = null;
    for (final Map.Entry<ValueSpecification, ValueRequirement> input : inputs.entrySet()) {
      final ValueProperties constraints = input.getValue().getConstraints();
      if (input.getValue().getValueName().equals(ValueRequirementNames.FORWARD_CURVE)) {
        if (constraints.getValues(CURVE_CALCULATION_METHOD) != null) {
          final Set<String> forwardCurveCalculationMethodNames = constraints.getValues(CURVE_CALCULATION_METHOD);
          if (forwardCurveCalculationMethodNames == null || forwardCurveCalculationMethodNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique forward curve calculation method name");
          }
          forwardCurveCalculationMethod = forwardCurveCalculationMethodNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_FORWARD_CURVE_INTERPOLATOR) != null) {
          final Set<String> forwardCurveInterpolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_INTERPOLATOR);
          if (forwardCurveInterpolatorNames == null || forwardCurveInterpolatorNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique forward curve interpolator name");
          }
          forwardCurveInterpolator = forwardCurveInterpolatorNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR) != null) {
          final Set<String> forwardCurveLeftExtrapolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR);
          if (forwardCurveLeftExtrapolatorNames == null || forwardCurveLeftExtrapolatorNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique forward curve left extrapolator name");
          }
          forwardCurveLeftExtrapolator = forwardCurveLeftExtrapolatorNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR) != null) {
          final Set<String> forwardCurveRightExtrapolatorNames = constraints.getValues(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR);
          if (forwardCurveRightExtrapolatorNames == null || forwardCurveRightExtrapolatorNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique forward curve right extrapolator name");
          }
          forwardCurveRightExtrapolator = forwardCurveRightExtrapolatorNames.iterator().next();
        }
      } else if (input.getValue().getValueName().equals(ValueRequirementNames.LOCAL_VOLATILITY_SURFACE)) {
        if (constraints.getValues(ValuePropertyNames.SURFACE) != null) {
          final Set<String> surfaceNames = constraints.getValues(ValuePropertyNames.SURFACE);
          if (surfaceNames == null || surfaceNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique surface name");
          }
          surfaceName = surfaceNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_SURFACE_TYPE) != null) {
          final Set<String> surfaceTypeNames = constraints.getValues(PROPERTY_SURFACE_TYPE);
          if (surfaceTypeNames == null || surfaceTypeNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique surface type name");
          }
          surfaceType = surfaceTypeNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_X_AXIS) != null) {
          final Set<String> xAxisNames = constraints.getValues(PROPERTY_X_AXIS);
          if (xAxisNames == null || xAxisNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique x-axis property name");
          }
          xAxis = xAxisNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_Y_AXIS) != null) {
          final Set<String> yAxisNames = constraints.getValues(PROPERTY_Y_AXIS);
          if (yAxisNames == null || yAxisNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique y-axis property name");
          }
          yAxis = yAxisNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_LAMBDA) != null) {
          final Set<String> lambdaNames = constraints.getValues(PROPERTY_LAMBDA);
          if (lambdaNames == null || lambdaNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique lambda property name");
          }
          lambda = lambdaNames.iterator().next();
        }
        if (constraints.getValues(PROPERTY_H) != null) {
          final Set<String> hNames = constraints.getValues(PROPERTY_H);
          if (hNames == null || hNames.size() != 1) {
            throw new OpenGammaRuntimeException("Missing or non-unique h name");
          }
          h = hNames.iterator().next();
        }
      }
    }
    assert surfaceType != null;
    assert xAxis != null;
    assert yAxis != null;
    assert lambda != null;
    assert forwardCurveCalculationMethod != null;
    assert h != null;
    assert forwardCurveInterpolator != null;
    assert forwardCurveLeftExtrapolator != null;
    assert forwardCurveRightExtrapolator != null;
    return Collections.singleton(getResultSpec(target, surfaceName, surfaceType, xAxis, yAxis, lambda, forwardCurveCalculationMethod, h, forwardCurveInterpolator,
        forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator));
  }

  protected abstract EuropeanVanillaOption getOption(final FinancialSecurity security, final ZonedDateTime date);

  //TODO shouldn't need to do this - write a fudge builder for the data bundle and have it as an input
  protected abstract SmileSurfaceDataBundle getData(final FunctionInputs inputs, final ValueRequirement volDataRequirement, final ValueRequirement forwardCurveRequirement);

  protected abstract Object getResult(final LocalVolatilityForwardPDEGreekCalculator<?> calculator, final LocalVolatilitySurface<?> localVolatilitySurface,
      final ForwardCurve forwardCurve, final SmileSurfaceDataBundle data, final EuropeanVanillaOption option);

  protected abstract String getResultName();

  private ValueRequirement getVolatilitySurfaceRequirement(final String surfaceName, final String surfaceType, final String xAxis, final String yAxis, final String lambda,
      final String h, final String forwardCurveCalculationMethod, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator, final UniqueId uid) {
    final ValueProperties properties = getLocalVolSurfaceProperties(surfaceName, surfaceType, xAxis, yAxis, lambda, h, forwardCurveCalculationMethod, forwardCurveInterpolator,
        forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator);
    return new ValueRequirement(ValueRequirementNames.LOCAL_VOLATILITY_SURFACE, ComputationTargetType.PRIMITIVE, uid, properties);
  }

  private ValueProperties getLocalVolSurfaceProperties(final String surfaceName, final String surfaceType, final String xAxis, final String yAxis, final String lambda, final String h,
      final String forwardCurveCalculationMethod, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator, final String forwardCurveRightExtrapolator) {
    return ValueProperties.builder()
        .with(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE, _instrumentType)
        .with(ValuePropertyNames.SURFACE, surfaceName)
        .with(PROPERTY_SURFACE_TYPE, surfaceType)
        .with(PROPERTY_X_AXIS, xAxis)
        .with(PROPERTY_Y_AXIS, yAxis)
        .with(PROPERTY_LAMBDA, lambda)
        .with(CURVE_CALCULATION_METHOD, forwardCurveCalculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator)
        .with(PROPERTY_H, h).get();
  }

  private ValueRequirement getForwardCurveRequirement(final String calculationMethod, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator, final UniqueId uid) {
    final ValueProperties properties = ValueProperties.builder()
        .with(ValuePropertyNames.CURVE_CALCULATION_METHOD, calculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator).get();
    return new ValueRequirement(ValueRequirementNames.FORWARD_CURVE, ComputationTargetType.PRIMITIVE, uid, properties);
  }

  private ValueProperties getResultProperties(final String definitionName, final String surfaceType, final String xAxis, final String yAxis, final String lambda,
      final String forwardCurveCalculationMethod, final String h, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator) {
    return createValueProperties()
        .with(ValuePropertyNames.CALCULATION_METHOD, LocalVolatilityPDEValuePropertyNames.LOCAL_VOLATILITY_METHOD)
        .with(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE, _instrumentType)
        .with(ValuePropertyNames.SURFACE, definitionName)
        .with(PROPERTY_SURFACE_TYPE, surfaceType)
        .with(PROPERTY_X_AXIS, xAxis)
        .with(PROPERTY_Y_AXIS, yAxis)
        .with(PROPERTY_LAMBDA, lambda)
        .with(CURVE_CALCULATION_METHOD, forwardCurveCalculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator)
        .withAny(PROPERTY_THETA)
        .withAny(PROPERTY_TIME_STEPS)
        .withAny(PROPERTY_SPACE_STEPS)
        .withAny(PROPERTY_TIME_GRID_BUNCHING)
        .withAny(PROPERTY_SPACE_GRID_BUNCHING)
        .withAny(PROPERTY_MAX_MONEYNESS)
        .with(PROPERTY_H, h)
        .withAny(PROPERTY_PDE_DIRECTION).get();
  }

  private ValueProperties getResultProperties(final String definitionName, final String surfaceType, final String xAxis, final String yAxis, final String lambda,
      final String forwardCurveCalculationMethod, final String h, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator, final String theta, final String timeSteps, final String spaceSteps, final String timeGridBunching,
      final String spaceGridBunching, final String maxMoneyness, final String pdeDirection) {
    return createValueProperties()
        .with(ValuePropertyNames.CALCULATION_METHOD, LocalVolatilityPDEValuePropertyNames.LOCAL_VOLATILITY_METHOD)
        .with(RawVolatilitySurfaceDataFunction.PROPERTY_SURFACE_INSTRUMENT_TYPE, _instrumentType)
        .with(ValuePropertyNames.SURFACE, definitionName)
        .with(PROPERTY_SURFACE_TYPE, surfaceType)
        .with(PROPERTY_X_AXIS, xAxis)
        .with(PROPERTY_Y_AXIS, yAxis)
        .with(PROPERTY_LAMBDA, lambda)
        .with(CURVE_CALCULATION_METHOD, forwardCurveCalculationMethod)
        .with(PROPERTY_FORWARD_CURVE_INTERPOLATOR, forwardCurveInterpolator)
        .with(PROPERTY_FORWARD_CURVE_LEFT_EXTRAPOLATOR, forwardCurveLeftExtrapolator)
        .with(PROPERTY_FORWARD_CURVE_RIGHT_EXTRAPOLATOR, forwardCurveRightExtrapolator)
        .with(PROPERTY_THETA, theta)
        .with(PROPERTY_TIME_STEPS, timeSteps)
        .with(PROPERTY_SPACE_STEPS, spaceSteps)
        .with(PROPERTY_TIME_GRID_BUNCHING, timeGridBunching)
        .with(PROPERTY_SPACE_GRID_BUNCHING, spaceGridBunching)
        .with(PROPERTY_MAX_MONEYNESS, maxMoneyness)
        .with(PROPERTY_H, h)
        .with(PROPERTY_PDE_DIRECTION, pdeDirection).get();
  }

  private ValueSpecification getResultSpec(final ComputationTarget target, final String definitionName, final String surfaceType, final String xAxis, final String yAxis,
      final String lambda, final String forwardCurveCalculationMethod, final String h, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator) {
    final ValueProperties properties = getResultProperties(definitionName, surfaceType, xAxis, yAxis, lambda, forwardCurveCalculationMethod, h, forwardCurveInterpolator,
        forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator);
    return new ValueSpecification(getResultName(), target.toSpecification(), properties);
  }

  private ValueSpecification getResultSpec(final ComputationTarget target, final String definitionName, final String surfaceType, final String xAxis, final String yAxis,
      final String lambda, final String forwardCurveCalculationMethod, final String h, final String forwardCurveInterpolator, final String forwardCurveLeftExtrapolator,
      final String forwardCurveRightExtrapolator, final String theta, final String timeSteps, final String spaceSteps, final String timeGridBunching,
      final String spaceGridBunching, final String maxMoneyness, final String pdeDirection) {
    final ValueProperties properties = getResultProperties(definitionName, surfaceType, xAxis, yAxis, lambda, forwardCurveCalculationMethod, h, forwardCurveInterpolator,
        forwardCurveLeftExtrapolator, forwardCurveRightExtrapolator, theta, timeSteps, spaceSteps, timeGridBunching, spaceGridBunching, maxMoneyness, pdeDirection);
    return new ValueSpecification(getResultName(), target.toSpecification(), properties);
  }
}
