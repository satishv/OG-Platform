/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.payments.method;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.interestrate.CashFlowEquivalentCalculator;
import com.opengamma.financial.interestrate.annuity.definition.AnnuityPaymentFixed;
import com.opengamma.financial.interestrate.payments.CapFloorCMSSpread;
import com.opengamma.financial.model.interestrate.G2ppPiecewiseConstantModel;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.model.interestrate.definition.G2ppPiecewiseConstantDataBundle;
import com.opengamma.math.function.Function2D;
import com.opengamma.math.integration.IntegratorRepeated2D;
import com.opengamma.math.integration.RungeKuttaIntegrator1D;
import com.opengamma.util.money.CurrencyAmount;

/**
 * Method to compute the present value of CMS spread cap/floor with the G2++ model by numerical integration.
 */
public class CapFloorCMSSpreadG2ppNumericalIntegrationMethod {

  /**
   * The model used in computations.
   */
  private static final G2ppPiecewiseConstantModel MODEL_G2PP = new G2ppPiecewiseConstantModel();
  /**
   * The cash flow equivalent calculator used in computations.
   */
  private static final CashFlowEquivalentCalculator CFEC = CashFlowEquivalentCalculator.getInstance();
  /**
   * Minimal number of integration steps in the integration.
   */
  private static final int NB_INTEGRATION = 10;

  /**
   * Computes the present value of a CMS spread by numerical integration.
   * @param cmsSpread The CMS spread cap/floor.
   * @param g2Data The curves and G2++ parameters.
   * @return The present value.
   */
  public CurrencyAmount presentValue(final CapFloorCMSSpread cmsSpread, final G2ppPiecewiseConstantDataBundle g2Data) {
    Validate.notNull(cmsSpread, "CMS spread");
    Validate.notNull(g2Data, "Yield curves and G2++ parameters");
    YieldAndDiscountCurve dsc = g2Data.getCurve(cmsSpread.getUnderlyingSwap1().getFixedLeg().getDiscountCurve());
    double strike = cmsSpread.getStrike();
    double theta = cmsSpread.getFixingTime();
    double tp = cmsSpread.getPaymentTime();
    double dftp = dsc.getDiscountFactor(tp);
    int[] nbCfFixed = new int[] {cmsSpread.getUnderlyingSwap1().getFixedLeg().getNumberOfPayments(), cmsSpread.getUnderlyingSwap2().getFixedLeg().getNumberOfPayments()};
    double[][] tFixed = new double[2][];
    double[][] dfFixed = new double[2][];
    double[][] discountedCashFlowFixed = new double[2][];
    double[][] tIbor = new double[2][];
    double[][] dfIbor = new double[2][];
    double[][] discountedCashFlowIbor = new double[2][];
    AnnuityPaymentFixed[] cfeIbor = new AnnuityPaymentFixed[] {CFEC.visit(cmsSpread.getUnderlyingSwap1().getSecondLeg(), g2Data), CFEC.visit(cmsSpread.getUnderlyingSwap2().getSecondLeg(), g2Data)};
    int[] nbCfIbor = new int[] {cfeIbor[0].getNumberOfPayments(), cfeIbor[1].getNumberOfPayments()};
    double[] notionalSwap = new double[] {cmsSpread.getUnderlyingSwap1().getFixedLeg().getNthPayment(0).getNotional(), cmsSpread.getUnderlyingSwap2().getFixedLeg().getNthPayment(0).getNotional()};
    // Swaps - Float
    for (int loopswap = 0; loopswap < 2; loopswap++) {
      tIbor[loopswap] = new double[nbCfIbor[loopswap]];
      dfIbor[loopswap] = new double[nbCfIbor[loopswap]];
      discountedCashFlowIbor[loopswap] = new double[nbCfIbor[loopswap]];

      for (int loopcf = 0; loopcf < nbCfIbor[loopswap]; loopcf++) {
        tIbor[loopswap][loopcf] = cfeIbor[loopswap].getNthPayment(loopcf).getPaymentTime();
        dfIbor[loopswap][loopcf] = dsc.getDiscountFactor(tIbor[loopswap][loopcf]);
        discountedCashFlowIbor[loopswap][loopcf] = dfIbor[loopswap][loopcf] * cfeIbor[loopswap].getNthPayment(loopcf).getAmount() / notionalSwap[loopswap];
      }

      tFixed[loopswap] = new double[nbCfFixed[loopswap]];
      dfFixed[loopswap] = new double[nbCfFixed[loopswap]];
      discountedCashFlowFixed[loopswap] = new double[nbCfFixed[loopswap]];
    }
    // Swap - Fixed
    for (int loopcf = 0; loopcf < nbCfFixed[0]; loopcf++) {
      tFixed[0][loopcf] = cmsSpread.getUnderlyingSwap1().getFixedLeg().getNthPayment(loopcf).getPaymentTime();
      dfFixed[0][loopcf] = dsc.getDiscountFactor(tFixed[0][loopcf]);
      discountedCashFlowFixed[0][loopcf] = dfFixed[0][loopcf] * cmsSpread.getUnderlyingSwap1().getFixedLeg().getNthPayment(loopcf).getPaymentYearFraction();
    }
    for (int loopcf = 0; loopcf < nbCfFixed[1]; loopcf++) {
      tFixed[1][loopcf] = cmsSpread.getUnderlyingSwap2().getFixedLeg().getNthPayment(loopcf).getPaymentTime();
      dfFixed[1][loopcf] = dsc.getDiscountFactor(tFixed[1][loopcf]);
      discountedCashFlowFixed[1][loopcf] = dfFixed[1][loopcf] * cmsSpread.getUnderlyingSwap2().getFixedLeg().getNthPayment(loopcf).getPaymentYearFraction();
    }
    // Model parameters
    double rhog2pp = g2Data.getG2ppParameter().getCorrelation();
    double[][] gamma = MODEL_G2PP.gamma(g2Data.getG2ppParameter(), 0, theta);
    double rhobar = rhog2pp * gamma[0][1] / Math.sqrt(gamma[0][0] * gamma[1][1]);
    double[][][] alphaFixed = new double[2][][];
    double[][] tau2Fixed = new double[2][];
    double[][][] alphaIbor = new double[2][][];
    double[][] tau2Ibor = new double[2][];
    for (int loopswap = 0; loopswap < 2; loopswap++) {
      double[][] hthetaFixed = MODEL_G2PP.volatilityMaturityPart(g2Data.getG2ppParameter(), theta, tFixed[loopswap]);
      alphaFixed[loopswap] = new double[2][nbCfFixed[loopswap]];
      tau2Fixed[loopswap] = new double[nbCfFixed[loopswap]];
      for (int loopcf = 0; loopcf < nbCfFixed[loopswap]; loopcf++) {
        alphaFixed[loopswap][0][loopcf] = Math.sqrt(gamma[0][0]) * hthetaFixed[0][loopcf];
        alphaFixed[loopswap][1][loopcf] = Math.sqrt(gamma[1][1]) * hthetaFixed[1][loopcf];
        tau2Fixed[loopswap][loopcf] = alphaFixed[loopswap][0][loopcf] * alphaFixed[loopswap][0][loopcf] + alphaFixed[loopswap][1][loopcf] * alphaFixed[loopswap][1][loopcf] + 2 * rhog2pp * gamma[0][1]
            * hthetaFixed[0][loopcf] * hthetaFixed[1][loopcf];
      }
      double[][] hthetaIbor = MODEL_G2PP.volatilityMaturityPart(g2Data.getG2ppParameter(), theta, tIbor[loopswap]);
      alphaIbor[loopswap] = new double[2][nbCfIbor[loopswap]];
      tau2Ibor[loopswap] = new double[nbCfIbor[loopswap]];
      for (int loopcf = 0; loopcf < nbCfIbor[loopswap]; loopcf++) {
        alphaIbor[loopswap][0][loopcf] = Math.sqrt(gamma[0][0]) * hthetaIbor[0][loopcf];
        alphaIbor[loopswap][1][loopcf] = Math.sqrt(gamma[1][1]) * hthetaIbor[1][loopcf];
        tau2Ibor[loopswap][loopcf] = alphaIbor[loopswap][0][loopcf] * alphaIbor[loopswap][0][loopcf] + alphaIbor[loopswap][1][loopcf] * alphaIbor[loopswap][1][loopcf] + 2 * rhog2pp * gamma[0][1]
            * hthetaIbor[0][loopcf] * hthetaIbor[1][loopcf];
      }
    }
    double[] hthetaTp = MODEL_G2PP.volatilityMaturityPart(g2Data.getG2ppParameter(), theta, tp);
    double[] alphaTp = new double[] {Math.sqrt(gamma[0][0]) * hthetaTp[0], Math.sqrt(gamma[1][1]) * hthetaTp[1]};
    double tau2Tp = alphaTp[0] * alphaTp[0] + alphaTp[1] * alphaTp[1] + 2 * rhog2pp * gamma[0][1] * hthetaTp[0] * hthetaTp[1];
    // Integration
    final SpreadIntegrant integrant = new SpreadIntegrant(discountedCashFlowFixed, alphaFixed, tau2Fixed, discountedCashFlowIbor, alphaIbor, tau2Ibor, alphaTp, tau2Tp, rhobar, strike,
        cmsSpread.isCap());
    final double limit = 10.0;
    final double absoluteTolerance = 1.0E-5;
    final double relativeTolerance = 1.0E-5;
    final RungeKuttaIntegrator1D integrator1D = new RungeKuttaIntegrator1D(absoluteTolerance, relativeTolerance, NB_INTEGRATION);
    IntegratorRepeated2D integrator2D = new IntegratorRepeated2D(integrator1D);
    double pv = 0.0;
    try {
      pv = 1.0 / (2.0 * Math.PI * Math.sqrt(1 - rhobar * rhobar)) * integrator2D.integrate(integrant, new Double[] {-limit, -limit}, new Double[] {limit, limit});
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return CurrencyAmount.of(cmsSpread.getCurrency(), dftp * pv * cmsSpread.getNotional() * cmsSpread.getPaymentYearFraction());
  }

  /**
   * Inner class to implement the integration used in price replication.
   */
  private class SpreadIntegrant extends Function2D<Double, Double> {

    private final double[][] _discountedCashFlowFixed;
    private final double[][][] _alphaFixed;
    private final double[][] _tau2Fixed;
    private final double[][] _discountedCashFlowIbor;
    private final double[][][] _alphaIbor;
    private final double[][] _tau2Ibor;
    private final double[] _alphaTp;
    private final double _tau2Tp;
    private final double _rhobar;
    private final double _strike;
    private final double _omega;

    public SpreadIntegrant(final double[][] discountedCashFlowFixed, final double[][][] alphaFixed, final double[][] tau2Fixed, final double[][] discountedCashFlowIbor, final double[][][] alphaIbor,
        final double[][] tau2Ibor, final double[] alphaTp, double tau2Tp, final double rhobar, double strike, boolean isCall) {
      _discountedCashFlowFixed = discountedCashFlowFixed;
      _alphaFixed = alphaFixed;
      _tau2Fixed = tau2Fixed;
      _discountedCashFlowIbor = discountedCashFlowIbor;
      _alphaIbor = alphaIbor;
      _tau2Ibor = tau2Ibor;
      _alphaTp = alphaTp;
      _tau2Tp = tau2Tp;
      _rhobar = rhobar;
      _strike = strike;
      _omega = (isCall ? 1.0 : -1.0);
    }

    @Override
    public Double evaluate(final Double x0, final Double x1) {
      double[] rate = new double[2];
      double[] x = new double[] {x0, x1};
      rate[0] = MODEL_G2PP.swapRate(x, _discountedCashFlowFixed[0], _alphaFixed[0], _tau2Fixed[0], _discountedCashFlowIbor[0], _alphaIbor[0], _tau2Ibor[0]);
      rate[1] = MODEL_G2PP.swapRate(x, _discountedCashFlowFixed[1], _alphaFixed[1], _tau2Fixed[1], _discountedCashFlowIbor[1], _alphaIbor[1], _tau2Ibor[1]);
      //      double[] rate = new double[2];
      //      double[] resultFixed = new double[2];
      //      double[] resultIbor = new double[2];
      //      for (int loopswap = 0; loopswap < 2; loopswap++) {
      //        for (int loopcf = 0; loopcf < _discountedCashFlowFixed[loopswap].length; loopcf++) {
      //          resultFixed[loopswap] += _discountedCashFlowFixed[loopswap][loopcf]
      //              * Math.exp(-_alphaFixed[loopswap][0][loopcf] * x0 - _alphaFixed[loopswap][1][loopcf] * x1 - _tau2Fixed[loopswap][loopcf] / 2.0);
      //        }
      //        for (int loopcf = 0; loopcf < _discountedCashFlowIbor[loopswap].length; loopcf++) {
      //          resultIbor[loopswap] += _discountedCashFlowIbor[loopswap][loopcf]
      //              * Math.exp(-_alphaIbor[loopswap][0][loopcf] * x0 - _alphaIbor[loopswap][1][loopcf] * x1 - _tau2Ibor[loopswap][loopcf] / 2.0);
      //        }
      //        rate[loopswap] = -resultIbor[loopswap] / resultFixed[loopswap];
      //      }

      double densityPart = -(x0 * x0 + x1 * x1 - 2 * _rhobar * x0 * x1) / (2.0 * (1 - _rhobar * _rhobar));
      double discounting = Math.exp(-_alphaTp[0] * x0 - _alphaTp[1] * x1 - _tau2Tp / 2.0 + densityPart);
      return discounting * Math.max(_omega * (rate[0] - rate[1] - _strike), 0.0);
    }

  }

}