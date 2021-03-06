/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.equity.future.derivative;

import com.opengamma.util.money.Currency;

/**
 * A cash-settled futures contract on the index of the *dividends* of a given stock market index on the _timeToFixing
 */
public class EquityIndexDividendFuture extends EquityFuture {

  public EquityIndexDividendFuture(final double timeToFixing, final double timeToDelivery, final double strike, final Currency currency, final double unitValue) {
    super(timeToFixing, timeToDelivery, strike, currency, unitValue);
  }

}
