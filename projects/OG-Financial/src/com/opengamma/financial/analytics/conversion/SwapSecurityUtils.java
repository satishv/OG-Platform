/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.conversion;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.financial.analytics.fixedincome.InterestRateInstrumentType;
import com.opengamma.financial.security.swap.FixedInterestRateLeg;
import com.opengamma.financial.security.swap.FloatingInterestRateLeg;
import com.opengamma.financial.security.swap.FloatingRateType;
import com.opengamma.financial.security.swap.FloatingSpreadIRLeg;
import com.opengamma.financial.security.swap.SwapLeg;
import com.opengamma.financial.security.swap.SwapSecurity;

/**
 * 
 */
public class SwapSecurityUtils {

  //TODO doesn't handle cross-currency swaps yet
  public static InterestRateInstrumentType getSwapType(final SwapSecurity security) {
    final SwapLeg payLeg = security.getPayLeg();
    final SwapLeg receiveLeg = security.getReceiveLeg();
    if (!payLeg.getRegionId().equals(receiveLeg.getRegionId())) {
      throw new OpenGammaRuntimeException("Pay and receive legs must be from same region");
    }
    if (payLeg instanceof FixedInterestRateLeg && receiveLeg instanceof FloatingInterestRateLeg) {
      
      final FloatingInterestRateLeg floatingLeg = (FloatingInterestRateLeg) receiveLeg;
      if (floatingLeg instanceof FloatingSpreadIRLeg) {
        return InterestRateInstrumentType.SWAP_FIXED_IBOR_WITH_SPREAD;
      } 
      FloatingRateType floatingRateType = floatingLeg.getFloatingRateType();
      switch (floatingRateType) {
        case IBOR:
          return InterestRateInstrumentType.SWAP_FIXED_IBOR;
        case CMS:
          return InterestRateInstrumentType.SWAP_FIXED_CMS;
        default:
          throw new OpenGammaRuntimeException("Unsupported Floating rate type: " + floatingRateType);
      }
    } else if (payLeg instanceof FloatingInterestRateLeg && receiveLeg instanceof FixedInterestRateLeg) {
      final FloatingInterestRateLeg floatingLeg = (FloatingInterestRateLeg) payLeg;
      
      if (floatingLeg instanceof FloatingSpreadIRLeg) {
        return InterestRateInstrumentType.SWAP_FIXED_IBOR_WITH_SPREAD;
      } 
      FloatingRateType floatingRateType = floatingLeg.getFloatingRateType();
      switch (floatingRateType) {
        case IBOR:
          return InterestRateInstrumentType.SWAP_FIXED_IBOR;
        case CMS:
          return InterestRateInstrumentType.SWAP_FIXED_CMS;
        default:
          throw new OpenGammaRuntimeException("Unsupported Floating rate type: " + floatingRateType);
      }
    }
    if (payLeg instanceof FloatingInterestRateLeg && receiveLeg instanceof FloatingInterestRateLeg) {
      final FloatingInterestRateLeg payLeg1 = (FloatingInterestRateLeg) payLeg;
      final FloatingInterestRateLeg receiveLeg1 = (FloatingInterestRateLeg) receiveLeg;
      if (payLeg1.getFloatingRateType().isIbor()) {
        if (receiveLeg1.getFloatingRateType().isIbor()) {
          return InterestRateInstrumentType.SWAP_IBOR_IBOR;
        }
        return InterestRateInstrumentType.SWAP_IBOR_CMS;
      }
      if (receiveLeg1.getFloatingRateType().isIbor()) {
        return InterestRateInstrumentType.SWAP_IBOR_CMS;
      }
      return InterestRateInstrumentType.SWAP_CMS_CMS;
    }
    throw new OpenGammaRuntimeException("Can only handle fixed-floating (pay and receive) swaps and floating-floating swaps");
  }

  public static boolean payFixed(final SwapSecurity security) {
    final SwapLeg payLeg = security.getPayLeg();
    final SwapLeg receiveLeg = security.getReceiveLeg();
    if (payLeg instanceof FixedInterestRateLeg && receiveLeg instanceof FloatingInterestRateLeg) {
      return true;
    }
    if (payLeg instanceof FloatingInterestRateLeg && receiveLeg instanceof FixedInterestRateLeg) {
      return false;
    }
    throw new OpenGammaRuntimeException("Swap was not fixed / floating");
  }
}
