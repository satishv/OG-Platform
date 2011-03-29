/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.language.convert;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.language.context.ContextTest;
import com.opengamma.language.context.SessionContext;
import com.opengamma.language.definition.JavaTypeInfo;
import com.opengamma.language.invoke.TypeConverter;

/* package */class AbstractConverterTest {

  private static final Logger s_logger = LoggerFactory.getLogger(AbstractConverterTest.class);

  private final SessionContext _sessionContext = ContextTest.createTestSessionContext();

  protected SessionContext getSessionContext() {
    return _sessionContext;
  }

  protected <T, J> void assertValidConversion(final TypeConverter converter, final T value,
      final JavaTypeInfo<J> target, final J expected) {
    final ValueConversionContext context = new ValueConversionContext(getSessionContext());
    converter.convertValue(context, value, target);
    if (context.isFailed()) {
      s_logger.warn("Can't convert from {}/{} to {}, expected {}", new Object[] {value.getClass(), value, target, expected });
      fail();
    }
    assertNotNull(value);
    final Object result = context.getResult();
    if (!expected.equals(result)) {
      s_logger.warn("Bad conversion from {}/{} to {}, expected {}, got {}", new Object[] {value.getClass(), value, target, expected, result });
      fail();
    }
  }

  protected <T, J> void assertInvalidConversion(final TypeConverter converter, final T value,
      final JavaTypeInfo<J> target) {
    final ValueConversionContext context = new ValueConversionContext(getSessionContext());
    converter.convertValue(context, value, target);
    if (!context.isFailed()) {
      final Object result = context.getResult();
      s_logger.warn("Shouldn't convert from {}/{} to {}, got {}", new Object[] {value.getClass(), value, target, result });
      fail();
    }
  }

  protected <J> void assertConversionCount(final int expected, final TypeConverter converter,
      final JavaTypeInfo<J> target) {
    final List<JavaTypeInfo<?>> conversions = converter.getConversionsTo(target);
    assertNotNull(conversions);
    assertEquals(expected, conversions.size());
  }

}