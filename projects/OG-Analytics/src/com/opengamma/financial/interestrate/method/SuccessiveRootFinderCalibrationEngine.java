/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate.method;

/**
 * Calibration engine calibrating successively the instruments in the basket trough a root-finding process.
 */
public abstract class SuccessiveRootFinderCalibrationEngine extends CalibrationEngine {

  /**
   * The calibration objective.
   */
  private final SuccessiveRootFinderCalibrationObjective _calibrationObjective;

  /**
   * The constructor.
   * @param calibrationObjective The calibration objective.
   */
  public SuccessiveRootFinderCalibrationEngine(final SuccessiveRootFinderCalibrationObjective calibrationObjective) {
    _calibrationObjective = calibrationObjective;
  }

  /**
   * Gets the calibration objective.
   * @return The calibration objective.
   */
  public SuccessiveRootFinderCalibrationObjective getCalibrationObjective() {
    return _calibrationObjective;
  }

}
