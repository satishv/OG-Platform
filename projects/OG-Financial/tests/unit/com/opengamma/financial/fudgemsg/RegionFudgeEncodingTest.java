/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.fudgemsg;

import static org.testng.AssertJUnit.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.opengamma.core.region.Region;
import com.opengamma.core.region.RegionUtils;
import com.opengamma.util.i18n.Country;

@Test(enabled = false)
public class RegionFudgeEncodingTest extends FinancialTestBase {

  private final Logger s_logger = LoggerFactory.getLogger(getClass());

  private Region getRef() {
    return getRegionSource().getHighestLevelRegion(RegionUtils.countryRegionId(Country.GB));
  }

  public void testCycle() {
    final Region ref = getRef();
    final Region cycledRef = cycleObject(Region.class, ref);
    s_logger.info("pre = " + ref);
    s_logger.info("post=" + cycledRef);
    assertEquals(ref, cycledRef);
  }

}
