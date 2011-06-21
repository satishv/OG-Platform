/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.model.finitedifference.applications;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.model.finitedifference.ExponentialMeshing;
import com.opengamma.financial.model.finitedifference.HyperbolicMeshing;
import com.opengamma.financial.model.finitedifference.MarkovChainApprox;
import com.opengamma.financial.model.finitedifference.MeshingFunction;
import com.opengamma.financial.model.finitedifference.PDEFullResults1D;
import com.opengamma.financial.model.finitedifference.PDEGrid1D;
import com.opengamma.financial.model.interestrate.curve.ForwardCurve;
import com.opengamma.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.financial.model.option.pricing.analytic.formula.BlackFunctionData;
import com.opengamma.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.financial.model.volatility.BlackImpliedVolatilityFormula;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.interpolation.DoubleQuadraticInterpolator1D;
import com.opengamma.math.interpolation.GridInterpolator2D;
import com.opengamma.math.interpolation.data.Interpolator1DDoubleQuadraticDataBundle;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.minimization.DoubleRangeLimitTransform;
import com.opengamma.math.minimization.ParameterLimitsTransform;
import com.opengamma.math.minimization.ParameterLimitsTransform.LimitType;
import com.opengamma.math.minimization.SingleRangeLimitTransform;
import com.opengamma.math.minimization.TransformParameters;
import com.opengamma.math.statistics.leastsquare.LeastSquareResults;
import com.opengamma.math.statistics.leastsquare.NonLinearLeastSquare;
import com.opengamma.util.tuple.DoublesPair;
import com.opengamma.util.tuple.ObjectsPair;
import com.opengamma.util.tuple.Pair;

/**
 * 
 */
public class TwoStateMarkovChainFitter {
  private static final BlackImpliedVolatilityFormula BLACK_IMPLIED_VOL = new BlackImpliedVolatilityFormula();
  private static final DoubleQuadraticInterpolator1D INTERPOLATOR_1D = new DoubleQuadraticInterpolator1D();
  private static final GridInterpolator2D<Interpolator1DDoubleQuadraticDataBundle, Interpolator1DDoubleQuadraticDataBundle> GRID_INTERPOLATOR2D = new GridInterpolator2D<Interpolator1DDoubleQuadraticDataBundle, Interpolator1DDoubleQuadraticDataBundle>(
      INTERPOLATOR_1D,
      INTERPOLATOR_1D);
  private static final TransformParameters TRANSFORMS;

  static {
    final ParameterLimitsTransform[] trans = new ParameterLimitsTransform[6];
    trans[0] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
    trans[1] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
    //trans[2] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
    //trans[3] = new SingleRangeLimitTransform(0, LimitType.GREATER_THAN);
    //    trans[0] = new DoubleRangeLimitTransform(0.1, 0.5);
    //trans[1] = new DoubleRangeLimitTransform(0.0, 0.7);
    trans[2] = new DoubleRangeLimitTransform(0.1, 5.0);//try to keep transition rates physical 
    trans[3] = new DoubleRangeLimitTransform(0.1, 5.0);
    trans[4] = new DoubleRangeLimitTransform(0.0, 1.0);
    trans[5] = new DoubleRangeLimitTransform(0.0, 2.0);
    // trans[6] = new DoubleRangeLimitTransform(0.0, 2.0);
    TRANSFORMS = new TransformParameters(new DoubleMatrix1D(new double[6]), trans, new BitSet());
  }

  private final double _theta;

  public TwoStateMarkovChainFitter() {
    _theta = 0.5;
  }

  public TwoStateMarkovChainFitter(final double theta) {
    _theta = theta;
  }

  public LeastSquareResults fit(final ForwardCurve forward, final List<Pair<double[], Double>> marketVols, DoubleMatrix1D initialGuess) {

    Validate.isTrue(initialGuess.getNumberOfElements() == TRANSFORMS.getNumberOfFunctionParameters());
    TRANSFORMS.transform(initialGuess);

    final int nMarketValues = marketVols.size();
    double tminT = Double.POSITIVE_INFINITY;
    double tminK = Double.POSITIVE_INFINITY;
    double tmaxT = 0;
    double tmaxK = 0;

    for (int i = 0; i < nMarketValues; i++) {
      double[] tk = marketVols.get(i).getFirst();

      if (tk[0] > tmaxT) {
        tmaxT = tk[0];
      }
      if (tk[0] < tminT) {
        tminT = tk[0];
      }
      if (tk[1] > tmaxK) {
        tmaxK = tk[1];
      }
      if (tk[1] < tminK) {
        tminK = tk[1];
      }
    }

    final double minT = 0.6 * tminT;
    final double minK = 0.9 * tminK;
    final double maxT = 1.0 * tmaxT;
    final double maxK = 1.1 * tmaxK;

    int tNodes = 20;
    int xNodes = 100;

    //TODO remove hard coded grid
    MeshingFunction timeMesh = new ExponentialMeshing(0, tmaxT, tNodes, 5.0);
    MeshingFunction spaceMesh = new HyperbolicMeshing(0, 10.0 * forward.getForward(maxT), forward.getSpot(), xNodes, 0.01);
    final PDEGrid1D grid = new PDEGrid1D(timeMesh, spaceMesh);

    Function1D<DoubleMatrix1D, DoubleMatrix1D> funcAppox = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        DoubleMatrix1D y = TRANSFORMS.inverseTransform(x);
        double vol1 = y.getEntry(0);
        double deltaVol = y.getEntry(1);
        double lambda12 = y.getEntry(2);
        double lambda21 = y.getEntry(3);
        double p0 = y.getEntry(4);
        double beta = y.getEntry(5);

        double[] modVols = new double[nMarketValues];
        for (int i = 0; i < nMarketValues; i++) {
          double[] temp = marketVols.get(i).getFirst();
          double t = temp[0];
          double k = temp[1];
          EuropeanVanillaOption option = new EuropeanVanillaOption(k, t, true);
          BlackFunctionData data = new BlackFunctionData(forward.getForward(t), 1.0, 0.0);
          MarkovChainApprox mca = new MarkovChainApprox(vol1, vol1 + deltaVol, lambda12, lambda21, p0, t);
          double price = mca.priceCEV(data.getForward(), data.getDiscountFactor(), k, beta);
          try {
            modVols[i] = BLACK_IMPLIED_VOL.getImpliedVolatility(data, option, price);
          } catch (Exception e) {
            modVols[i] = 0.0;
            //System.out.println("arrrgggg");
          }
        }
        //debug(DataBundle);
        return new DoubleMatrix1D(modVols);
      }
    };

    Function1D<DoubleMatrix1D, DoubleMatrix1D> func = new Function1D<DoubleMatrix1D, DoubleMatrix1D>() {

      @Override
      public DoubleMatrix1D evaluate(DoubleMatrix1D x) {
        DoubleMatrix1D y = TRANSFORMS.inverseTransform(x);
        double vol1 = y.getEntry(0);
        double deltaVol = y.getEntry(1);
        double lambda12 = y.getEntry(2);
        double lambda21 = y.getEntry(3);
        double p0 = y.getEntry(4);
        double beta = y.getEntry(5);
        TwoStateMarkovChainDataBundle chainData = new TwoStateMarkovChainDataBundle(vol1, vol1 + deltaVol, lambda12, lambda21, p0, beta, beta);
        TwoStateMarkovChainPricer mc = new TwoStateMarkovChainPricer(forward, chainData);
        PDEFullResults1D res = mc.solve(grid, _theta);
        Map<DoublesPair, Double> data = PDEUtilityTools.priceToImpliedVol(forward, res, minT, maxT, minK, maxK);
        Map<Double, Interpolator1DDoubleQuadraticDataBundle> dataBundle = GRID_INTERPOLATOR2D.getDataBundle(data);
        double[] modVols = new double[nMarketValues];
        for (int i = 0; i < nMarketValues; i++) {
          double[] temp = marketVols.get(i).getFirst();
          DoublesPair tk = new DoublesPair(temp[0], temp[1]);
          try {
            modVols[i] = GRID_INTERPOLATOR2D.interpolate(dataBundle, tk);
          } catch (Exception e) {
            System.out.println("arrrgggg");
          }
        }
        //debug(DataBundle);
        return new DoubleMatrix1D(modVols);
      }

    };

    double[] mrkVols = new double[nMarketValues];
    double[] sigma = new double[nMarketValues];
    for (int i = 0; i < nMarketValues; i++) {
      mrkVols[i] = marketVols.get(i).getSecond();
      sigma[i] = 0.01; //1% error
    }

    NonLinearLeastSquare ls = new NonLinearLeastSquare();
    //solve approx first
    LeastSquareResults solverRes = ls.solve(new DoubleMatrix1D(mrkVols), new DoubleMatrix1D(sigma), funcAppox, TRANSFORMS.transform(initialGuess));
    // now solve pde model
    solverRes = ls.solve(new DoubleMatrix1D(mrkVols), new DoubleMatrix1D(sigma), func, solverRes.getParameters());
    return new LeastSquareResults(solverRes.getChiSq(), TRANSFORMS.inverseTransform(solverRes.getParameters()), solverRes.getCovariance());
  }

  public void debug(Map<Double, Interpolator1DDoubleQuadraticDataBundle> dataBundle) {
    for (int i = 0; i < 101; i++) {
      double k = 0. + 4.0 * i / 100.;
      System.out.print("\t" + k);
    }
    System.out.print("\n");

    for (int j = 0; j < 101; j++) {
      double t = 0.2 + 4.8 * j / 100.;
      System.out.print(t);
      for (int i = 0; i < 101; i++) {
        double k = 0. + 4.0 * i / 100.;
        // System.out.print("\t" + INTERPOLATOR.interpolate(DATABUNDLE, new double[] {t, k }));
        System.out.print("\t" + GRID_INTERPOLATOR2D.interpolate(dataBundle, new DoublesPair(t, k)));
      }
      System.out.print("\n");
    }
  }

  /**
   * Transforms the price data (in PDEFullResults1D form) to implied volatility in a form used by 2D interpolator 
   * @param forward
   * @param yield
   * @param prices
   * @return
   */
  private List<Pair<double[], Double>> transformData(final ForwardCurve forward, final YieldCurve yield, final PDEFullResults1D prices,
      final double minT, final double maxT, final double minK, final double maxK) {
    int xNodes = prices.getNumberSpaceNodes();
    int tNodes = prices.getNumberTimeNodes();
    int n = xNodes * tNodes;
    List<Pair<double[], Double>> out = new ArrayList<Pair<double[], Double>>(n);

    for (int i = 0; i < tNodes; i++) {
      double t = prices.getTimeValue(i);
      if (t >= minT && t <= maxT) {
        BlackFunctionData data = new BlackFunctionData(forward.getForward(t), yield.getDiscountFactor(t), 0);
        for (int j = 0; j < xNodes; j++) {
          double k = prices.getSpaceValue(j);
          if (k >= minK && k <= maxK) {
            double price = prices.getFunctionValue(j, i);
            EuropeanVanillaOption option = new EuropeanVanillaOption(k, t, true);
            try {
              double impVol = BLACK_IMPLIED_VOL.getImpliedVolatility(data, option, price);
              Pair<double[], Double> pair = new ObjectsPair<double[], Double>(
                  new double[] {prices.getTimeValue(i), prices.getSpaceValue(j) }, impVol);
              out.add(pair);
            } catch (Exception e) {
              System.out.println("can't find vol for strike: " + prices.getSpaceValue(j) + " and expiry " + prices.getTimeValue(i) + " . Not added to data set");
            }
          }
        }
      }
    }
    return out;
  }

  //  protected Map<DoublesPair, Double> priceToImpliedVol(final ForwardCurve forward, final PDEFullResults1D prices,
  //      final double minT, final double maxT, final double minK, final double maxK) {
  //    int xNodes = prices.getNumberSpaceNodes();
  //    int tNodes = prices.getNumberTimeNodes();
  //    int n = xNodes * tNodes;
  //    Map<DoublesPair, Double> out = new HashMap<DoublesPair, Double>(n);
  //    int count = tNodes * xNodes;
  //
  //    for (int i = 0; i < tNodes; i++) {
  //      double t = prices.getTimeValue(i);
  //      if (t >= minT && t <= maxT) {
  //        BlackFunctionData data = new BlackFunctionData(forward.getForward(t), forward.getSpot() / forward.getForward(t), 0);
  //        for (int j = 0; j < xNodes; j++) {
  //          double k = prices.getSpaceValue(j);
  //          if (k >= minK && k <= maxK) {
  //            double price = prices.getFunctionValue(j, i);
  //            EuropeanVanillaOption option = new EuropeanVanillaOption(k, t, true);
  //            try {
  //              double impVol = BLACK_IMPLIED_VOL.getImpliedVolatility(data, option, price);
  //              if (Math.abs(impVol) > 1e-15) {
  //                DoublesPair pair = new DoublesPair(prices.getTimeValue(i), prices.getSpaceValue(j));
  //                out.put(pair, impVol);
  //                count--;
  //              }
  //            } catch (Exception e) {
  //              // System.out.println("can't find vol for strike: " + prices.getSpaceValue(j) + " and expiry " + prices.getTimeValue(i) + " . Not added to data set");
  //            }
  //          }
  //        }
  //      }
  //    }
  //    //    if (count > 0) {
  //    //      System.err.println(count + " out of " + xNodes * tNodes + " data points removed");
  //    //    }
  //    return out;
  //  }

}