/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.option.pricing.analytic;

import static org.testng.AssertJUnit.assertEquals;

import javax.time.calendar.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.financial.model.option.definition.EuropeanVanillaOptionDefinition;
import com.opengamma.financial.model.option.definition.GapOptionDefinition;
import com.opengamma.financial.model.option.definition.OptionDefinition;
import com.opengamma.financial.model.option.definition.StandardOptionDataBundle;
import com.opengamma.financial.model.volatility.surface.VolatilitySurface;
import com.opengamma.math.curve.ConstantDoublesCurve;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.surface.ConstantDoublesSurface;
import com.opengamma.util.time.DateUtils;
import com.opengamma.util.time.Expiry;

/**
 * 
 */
public class GapOptionModelTest {
  private static final YieldAndDiscountCurve CURVE = new YieldCurve(ConstantDoublesCurve.from(0.09));
  private static final double B = 0.09;
  private static final VolatilitySurface SURFACE = new VolatilitySurface(ConstantDoublesSurface.from(0.2));
  private static final double SPOT = 50;
  private static final ZonedDateTime DATE = DateUtils.getUTCDate(2010, 7, 1);
  private static final double STRIKE = 50;
  private static final double PAYOFF_STRIKE = 57;
  private static final Expiry EXPIRY = new Expiry(DateUtils.getDateOffsetWithYearFraction(DATE, 0.5));
  private static final StandardOptionDataBundle DATA = new StandardOptionDataBundle(CURVE, B, SURFACE, SPOT, DATE);
  private static final AnalyticOptionModel<GapOptionDefinition, StandardOptionDataBundle> MODEL = new GapOptionModel();
  private static final AnalyticOptionModel<OptionDefinition, StandardOptionDataBundle> BSM = new BlackScholesMertonModel();
  private static final double EPS = 1e-12;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDefinition() {
    MODEL.getPricingFunction(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullData() {
    MODEL.getPricingFunction(new GapOptionDefinition(STRIKE, EXPIRY, true, PAYOFF_STRIKE)).evaluate((StandardOptionDataBundle) null);
  }

  @Test
  public void testAgainstBSM() {
    final StandardOptionDataBundle data = DATA;
    final Function1D<StandardOptionDataBundle, Double> call = MODEL.getPricingFunction(new GapOptionDefinition(STRIKE, EXPIRY, true, STRIKE));
    final Function1D<StandardOptionDataBundle, Double> put = MODEL.getPricingFunction(new GapOptionDefinition(STRIKE, EXPIRY, false, STRIKE));
    final Function1D<StandardOptionDataBundle, Double> bsmCall = BSM.getPricingFunction(new EuropeanVanillaOptionDefinition(STRIKE, EXPIRY, true));
    final Function1D<StandardOptionDataBundle, Double> bsmPut = BSM.getPricingFunction(new EuropeanVanillaOptionDefinition(STRIKE, EXPIRY, false));
    assertEquals(call.evaluate(data), bsmCall.evaluate(data), EPS);
    assertEquals(put.evaluate(data), bsmPut.evaluate(data), EPS);
  }

  @Test
  public void test() {
    assertEquals(MODEL.getPricingFunction(new GapOptionDefinition(STRIKE, EXPIRY, true, PAYOFF_STRIKE)).evaluate(DATA), -0.0053, 1e-4);
  }
}
