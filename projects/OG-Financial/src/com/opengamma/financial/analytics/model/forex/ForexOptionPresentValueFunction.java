/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.forex;

import java.util.Collections;
import java.util.Set;

import org.apache.commons.lang.Validate;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.forex.calculator.PresentValueBlackForexCalculator;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.model.option.definition.SmileDeltaTermStructureDataBundle;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;

/**
 * 
 */
public class ForexOptionPresentValueFunction extends ForexOptionFunction {
  private static final PresentValueBlackForexCalculator CALCULATOR = PresentValueBlackForexCalculator.getInstance();

  @Override
  protected Set<ComputedValue> getResult(final InstrumentDerivative fxOption, final SmileDeltaTermStructureDataBundle data, final FunctionInputs inputs, final ComputationTarget target,
      final String putFundingCurveName, final String putForwardCurveName, final String callFundingCurveName, final String callForwardCurveName, final String surfaceName) {
    final MultipleCurrencyAmount result = CALCULATOR.visit(fxOption, data);
    Validate.isTrue(result.size() == 1);
    final CurrencyAmount ca = result.getCurrencyAmounts()[0];
    final double amount = ca.getAmount();
    final ValueProperties.Builder properties = getResultProperties(putFundingCurveName, putForwardCurveName, callFundingCurveName, callForwardCurveName, surfaceName, target);
    final ValueSpecification spec = new ValueSpecification(getValueRequirementName(), target.toSpecification(), properties.get());
    return Collections.singleton(new ComputedValue(spec, amount));
  }

  @Override
  protected String getValueRequirementName() {
    return ValueRequirementNames.PRESENT_VALUE;
  }

}
