/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.language.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.opengamma.core.security.Security;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.id.UniqueId;
import com.opengamma.language.context.SessionContext;
import com.opengamma.language.definition.Categories;
import com.opengamma.language.definition.DefinitionAnnotater;
import com.opengamma.language.definition.JavaTypeInfo;
import com.opengamma.language.definition.MetaParameter;
import com.opengamma.language.error.InvokeInvalidArgumentException;
import com.opengamma.language.function.AbstractFunctionInvoker;
import com.opengamma.language.function.MetaFunction;
import com.opengamma.language.function.PublishedFunction;

/**
 * A function which fetches a security from a security source.
 */
public class FetchSecurityFunction extends AbstractFunctionInvoker implements PublishedFunction {

  /**
   * Default instance.
   */
  public static final FetchSecurityFunction INSTANCE = new FetchSecurityFunction();

  private final MetaFunction _meta;

  private static List<MetaParameter> parameters() {
    final MetaParameter identifiers = new MetaParameter("identifiers", JavaTypeInfo.builder(ExternalIdBundle.class).allowNull().get());
    final MetaParameter uniqueIdentifier = new MetaParameter("uniqueId", JavaTypeInfo.builder(UniqueId.class).allowNull().get());
    return Arrays.asList(identifiers, uniqueIdentifier);
  }

  private FetchSecurityFunction(final DefinitionAnnotater info) {
    super(info.annotate(parameters()));
    _meta = info.annotate(new MetaFunction(Categories.SECURITY, "FetchSecurity", getParameters(), this));
  }

  protected FetchSecurityFunction() {
    this(new DefinitionAnnotater(FetchSecurityFunction.class));
  }

  // AbstractFunctionInvoker

  @Override
  protected Object invokeImpl(final SessionContext sessionContext, final Object[] parameters) {
    final ExternalIdBundle identifiers = (ExternalIdBundle) parameters[0];
    final UniqueId uniqueId = (UniqueId) parameters[1];
    if (identifiers == null) {
      if (uniqueId == null) {
        throw new InvokeInvalidArgumentException(1, "Unique identifier must be specified if identifier bundle is omitted");
      } else {
        return sessionContext.getGlobalContext().getSecuritySource().getSecurity(uniqueId);
      }
    } else {
      if (uniqueId == null) {
        final Collection<Security> securities = sessionContext.getGlobalContext().getSecuritySource().getSecurities(identifiers);
        if (securities.size() == 1) {
          return securities.iterator().next();
        } else {
          return securities;
        }
      } else {
        throw new InvokeInvalidArgumentException(1, "Unique identifier must be omitted if identifier bundle is specified");
      }
    }
  }

  // PublishedFunction

  @Override
  public MetaFunction getMetaFunction() {
    return _meta;
  }

}
