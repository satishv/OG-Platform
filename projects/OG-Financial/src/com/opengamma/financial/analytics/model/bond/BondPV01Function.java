/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.bond;

import java.util.Set;

import javax.time.calendar.Clock;
import javax.time.calendar.ZonedDateTime;

import com.google.common.collect.Sets;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.core.security.Security;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.OpenGammaExecutionContext;
import com.opengamma.financial.analytics.conversion.BondSecurityConverter;
import com.opengamma.financial.analytics.ircurve.YieldCurveFunction;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.instrument.bond.BondFixedSecurityDefinition;
import com.opengamma.financial.interestrate.PV01Calculator;
import com.opengamma.financial.interestrate.YieldCurveBundle;
import com.opengamma.financial.interestrate.bond.definition.BondFixedSecurity;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.security.bond.BondSecurity;

/**
 * 
 */
public abstract class BondPV01Function extends AbstractFunction.NonCompiledInvoker {
  private static final PV01Calculator PV01_CALCULATOR = PV01Calculator.getInstance();

  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target, final Set<ValueRequirement> desiredValues) {
    final BondSecurity security = (BondSecurity) target.getSecurity();    
    final Object curveObject = inputs.getValue(ValueRequirementNames.YIELD_CURVE);
    String curveName = null;
    for (ValueRequirement desiredValue : desiredValues) {
      curveName = YieldCurveFunction.getCurveName(desiredValue);
      if (curveName != null) {
        break;
      }
    }
    if (curveName == null) {
      throw new NullPointerException("Curve name not specified as value constraint in " + desiredValues);
    }

    final Clock snapshotClock = executionContext.getValuationClock();
    final ZonedDateTime now = snapshotClock.zonedDateTime();
    final HolidaySource holidaySource = OpenGammaExecutionContext.getHolidaySource(executionContext);
    final ConventionBundleSource conventionSource = OpenGammaExecutionContext
        .getConventionBundleSource(executionContext);
    final RegionSource regionSource = OpenGammaExecutionContext.getRegionSource(executionContext);
    final BondSecurityConverter visitor = new BondSecurityConverter(holidaySource, conventionSource, regionSource);
    BondFixedSecurity bond = ((BondFixedSecurityDefinition) security.accept(visitor)).toDerivative(now, curveName);
    final YieldCurveBundle bundle;
    final YieldAndDiscountCurve curve = (YieldAndDiscountCurve) curveObject;
    bundle = new YieldCurveBundle(new String[] {curveName}, new YieldAndDiscountCurve[] {curve});
    double pv = PV01_CALCULATOR.visit(bond, bundle).get(curveName);
    final ValueSpecification specification = new ValueSpecification(ValueRequirementNames.PV01, target.toSpecification(), 
        createValueProperties().with(ValuePropertyNames.CURVE, curveName).get());
    return Sets.newHashSet(new ComputedValue(specification, pv)); 
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() == ComputationTargetType.SECURITY) {
      final Security security = target.getSecurity();
      return security instanceof BondSecurity;
    }
    return false;
  }

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.SECURITY;
  }

}
