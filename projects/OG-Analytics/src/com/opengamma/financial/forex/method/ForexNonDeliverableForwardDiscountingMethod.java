/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.forex.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.forex.derivative.Forex;
import com.opengamma.financial.forex.derivative.ForexNonDeliverableForward;
import com.opengamma.financial.interestrate.InstrumentDerivative;
import com.opengamma.financial.interestrate.InterestRateCurveSensitivity;
import com.opengamma.financial.interestrate.YieldCurveBundle;
import com.opengamma.util.money.Currency;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.tuple.DoublesPair;

/**
 * Pricing method for Forex non-deliverable forward transactions by discounting.
 */
public final class ForexNonDeliverableForwardDiscountingMethod implements ForexPricingMethod {

  /**
   * The method unique instance.
   */
  private static final ForexNonDeliverableForwardDiscountingMethod INSTANCE = new ForexNonDeliverableForwardDiscountingMethod();

  /**
   * Return the unique instance of the class.
   * @return The instance.
   */
  public static ForexNonDeliverableForwardDiscountingMethod getInstance() {
    return INSTANCE;
  }

  /**
   * Private constructor.
   */
  private ForexNonDeliverableForwardDiscountingMethod() {
  }

  /**
   * Computes the present value of the non-deliverable forward. The present value is in currency2 and equal to P*N*(1-X/F) where
   * <i>P</i> is the currency2 discount factor for the payment date, <i>N</i> is the notional, <i>X</i> is NDF rate and <i>F</i> the estimated forward exchange rate at the payment date.
   * @param ndf The non-deliverable forward.
   * @param curves The curve bundle (with FX rates).
   * @return The present value.
   */
  public MultipleCurrencyAmount presentValue(final ForexNonDeliverableForward ndf, final YieldCurveWithFXBundle curves) {
    double df2 = curves.getCurve(ndf.getDiscountingCurve2Name()).getDiscountFactor(ndf.getPaymentTime());
    double df1 = curves.getCurve(ndf.getDiscountingCurve1Name()).getDiscountFactor(ndf.getPaymentTime());
    double spot = curves.getFxRate(ndf.getCurrency2(), ndf.getCurrency1());
    double pv2 = ndf.getNotionalCurrency2() * (df2 - ndf.getExchangeRate() / spot * df1);
    return MultipleCurrencyAmount.of(ndf.getCurrency2(), pv2);
  }

  @Override
  public MultipleCurrencyAmount presentValue(InstrumentDerivative instrument, YieldCurveBundle curves) {
    Validate.isTrue(instrument instanceof ForexNonDeliverableForward, "Derivative should be ForexNonDeliverableForward");
    Validate.isTrue(curves instanceof YieldCurveWithFXBundle, "Bundle should contain FX rate");
    return presentValue((ForexNonDeliverableForward) instrument, (YieldCurveWithFXBundle) curves);
  }

  /**
   * Computes the currency exposure of the non-deliverable forward. The currency exposure is P_2 * N in currency2 and -P_1 * N * X in currency1 
   * where <i>P_2</i> is the currency2 discount factor for the payment date, <i>N</i> is the notional, <i>P_1</i> is the currency1 discount factor for the payment date
   * and <i>X</i> is NDF rate.
   * @param ndf The non-deliverable forward.
   * @param curves The curve bundle (with FX rates).
   * @return The currency exposure.
   */
  public MultipleCurrencyAmount currencyExposure(final ForexNonDeliverableForward ndf, final YieldCurveBundle curves) {
    double df2 = curves.getCurve(ndf.getDiscountingCurve2Name()).getDiscountFactor(ndf.getPaymentTime());
    double df1 = curves.getCurve(ndf.getDiscountingCurve1Name()).getDiscountFactor(ndf.getPaymentTime());
    double pv1 = -ndf.getNotionalCurrency2() * ndf.getExchangeRate() * df1;
    double pv2 = ndf.getNotionalCurrency2() * df2;
    return MultipleCurrencyAmount.of(new Currency[] {ndf.getCurrency1(), ndf.getCurrency2()}, new double[] {pv1, pv2});

  }

  @Override
  public MultipleCurrencyAmount currencyExposure(InstrumentDerivative instrument, YieldCurveBundle curves) {
    Validate.isTrue(instrument instanceof Forex, "Derivative should be ForexNonDeliverableForward");
    return currencyExposure((ForexNonDeliverableForward) instrument, curves);
  }

  /**
   * Computes the forward exchange rate associated to the NDF (1 Cyy2 = fwd Cyy1).
   * @param ndf The non-deliverable forward.
   * @param curves The curve bundle (with FX rates).
   * @return The forward rate.
   */
  public double forwardForexRate(final ForexNonDeliverableForward ndf, final YieldCurveWithFXBundle curves) {
    final double dfDelivery = curves.getCurve(ndf.getDiscountingCurve2Name()).getDiscountFactor(ndf.getPaymentTime());
    final double dfNonDelivery = curves.getCurve(ndf.getDiscountingCurve1Name()).getDiscountFactor(ndf.getPaymentTime());
    final double spot = curves.getFxRate(ndf.getCurrency2(), ndf.getCurrency1());
    return spot * dfDelivery / dfNonDelivery;
  }

  /**
   * The present value curve sensitivity for the non-deliverable forward.
   * @param ndf The non-deliverable forward.
   * @param curves The curve bundle (with FX rates).
   * @return The present value currency exposure.
   */
  public MultipleCurrencyInterestRateCurveSensitivity presentValueCurveSensitivity(final ForexNonDeliverableForward ndf, final YieldCurveBundle curves) {
    Validate.isTrue(curves instanceof YieldCurveWithFXBundle, "Bundle should contain FX rate");
    YieldCurveWithFXBundle curvesFX = (YieldCurveWithFXBundle) curves;
    double df2 = curves.getCurve(ndf.getDiscountingCurve2Name()).getDiscountFactor(ndf.getPaymentTime());
    double df1 = curves.getCurve(ndf.getDiscountingCurve1Name()).getDiscountFactor(ndf.getPaymentTime());
    double spot = curvesFX.getFxRate(ndf.getCurrency2(), ndf.getCurrency1());
    // Backward sweep
    double pvBar = 1.0;
    double df1Bar = -ndf.getNotionalCurrency2() * ndf.getExchangeRate() / spot * pvBar;
    double df2Bar = ndf.getNotionalCurrency2() * pvBar;
    final Map<String, List<DoublesPair>> resultMap = new HashMap<String, List<DoublesPair>>();
    final List<DoublesPair> listDiscounting1 = new ArrayList<DoublesPair>();
    listDiscounting1.add(new DoublesPair(ndf.getPaymentTime(), -ndf.getPaymentTime() * df1 * df1Bar));
    resultMap.put(ndf.getDiscountingCurve1Name(), listDiscounting1);
    final List<DoublesPair> listDiscounting2 = new ArrayList<DoublesPair>();
    listDiscounting2.add(new DoublesPair(ndf.getPaymentTime(), -ndf.getPaymentTime() * df2 * df2Bar));
    resultMap.put(ndf.getDiscountingCurve2Name(), listDiscounting2);
    final InterestRateCurveSensitivity result = new InterestRateCurveSensitivity(resultMap);
    return MultipleCurrencyInterestRateCurveSensitivity.of(ndf.getCurrency2(), result);
  }

  @Override
  public MultipleCurrencyInterestRateCurveSensitivity presentValueCurveSensitivity(final InstrumentDerivative instrument, final YieldCurveBundle curves) {
    Validate.isTrue(instrument instanceof ForexNonDeliverableForward, "Forex non-deliverable forward");
    return presentValueCurveSensitivity((ForexNonDeliverableForward) instrument, curves);
  }

}
