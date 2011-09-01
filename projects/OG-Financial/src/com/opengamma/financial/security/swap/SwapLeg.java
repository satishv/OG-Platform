/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.security.swap;

import java.io.Serializable;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.financial.convention.businessday.BusinessDayConvention;
import com.opengamma.financial.convention.daycount.DayCount;
import com.opengamma.financial.convention.frequency.Frequency;
import com.opengamma.id.ExternalId;

/**
 * Abstract base class for one leg in a swap.
 */
@BeanDefinition
public abstract class SwapLeg extends DirectBean implements Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The day count.
   */
  @PropertyDefinition(validate = "notNull")
  private DayCount _dayCount;
  /**
   * The frequency.
   */
  @PropertyDefinition(validate = "notNull")
  private Frequency _frequency;
  /**
   * The region external identifier.
   */
  @PropertyDefinition(validate = "notNull")
  private ExternalId _regionIdentifier;
  /**
   * The business day convention.
   */
  @PropertyDefinition(validate = "notNull")
  private BusinessDayConvention _businessDayConvention;
  /**
   * The notional.
   */
  @PropertyDefinition(validate = "notNull")
  private Notional _notional;

  /**
   * Creates an instance.
   */
  protected SwapLeg() {
  }

  /**
   * Creates an instance.
   * 
   * @param dayCount  the day count, not null
   * @param frequency  the frequency, not null
   * @param regionIdentifier  the region, not null
   * @param businessDayConvention  the business day convention, not null
   * @param notional  the notional, not null
   */
  protected SwapLeg(DayCount dayCount, Frequency frequency, ExternalId regionIdentifier, BusinessDayConvention businessDayConvention, Notional notional) {
    setDayCount(dayCount);
    setFrequency(frequency);
    setRegionIdentifier(regionIdentifier);
    setBusinessDayConvention(businessDayConvention);
    setNotional(notional);
  }

  //-------------------------------------------------------------------------
  /**
   * Accepts a visitor to manage traversal of the hierarchy.
   * 
   * @param <T> the result type of the visitor
   * @param visitor  the visitor, not null
   * @return the result
   */
  public abstract <T> T accept(SwapLegVisitor<T> visitor);

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SwapLeg}.
   * @return the meta-bean, not null
   */
  public static SwapLeg.Meta meta() {
    return SwapLeg.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(SwapLeg.Meta.INSTANCE);
  }

  @Override
  public SwapLeg.Meta metaBean() {
    return SwapLeg.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 1905311443:  // dayCount
        return getDayCount();
      case -70023844:  // frequency
        return getFrequency();
      case 19238589:  // regionIdentifier
        return getRegionIdentifier();
      case -1002835891:  // businessDayConvention
        return getBusinessDayConvention();
      case 1585636160:  // notional
        return getNotional();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 1905311443:  // dayCount
        setDayCount((DayCount) newValue);
        return;
      case -70023844:  // frequency
        setFrequency((Frequency) newValue);
        return;
      case 19238589:  // regionIdentifier
        setRegionIdentifier((ExternalId) newValue);
        return;
      case -1002835891:  // businessDayConvention
        setBusinessDayConvention((BusinessDayConvention) newValue);
        return;
      case 1585636160:  // notional
        setNotional((Notional) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  protected void validate() {
    JodaBeanUtils.notNull(_dayCount, "dayCount");
    JodaBeanUtils.notNull(_frequency, "frequency");
    JodaBeanUtils.notNull(_regionIdentifier, "regionIdentifier");
    JodaBeanUtils.notNull(_businessDayConvention, "businessDayConvention");
    JodaBeanUtils.notNull(_notional, "notional");
    super.validate();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SwapLeg other = (SwapLeg) obj;
      return JodaBeanUtils.equal(getDayCount(), other.getDayCount()) &&
          JodaBeanUtils.equal(getFrequency(), other.getFrequency()) &&
          JodaBeanUtils.equal(getRegionIdentifier(), other.getRegionIdentifier()) &&
          JodaBeanUtils.equal(getBusinessDayConvention(), other.getBusinessDayConvention()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    hash += hash * 31 + JodaBeanUtils.hashCode(getFrequency());
    hash += hash * 31 + JodaBeanUtils.hashCode(getRegionIdentifier());
    hash += hash * 31 + JodaBeanUtils.hashCode(getBusinessDayConvention());
    hash += hash * 31 + JodaBeanUtils.hashCode(getNotional());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return _dayCount;
  }

  /**
   * Sets the day count.
   * @param dayCount  the new value of the property, not null
   */
  public void setDayCount(DayCount dayCount) {
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this._dayCount = dayCount;
  }

  /**
   * Gets the the {@code dayCount} property.
   * @return the property, not null
   */
  public final Property<DayCount> dayCount() {
    return metaBean().dayCount().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the frequency.
   * @return the value of the property, not null
   */
  public Frequency getFrequency() {
    return _frequency;
  }

  /**
   * Sets the frequency.
   * @param frequency  the new value of the property, not null
   */
  public void setFrequency(Frequency frequency) {
    JodaBeanUtils.notNull(frequency, "frequency");
    this._frequency = frequency;
  }

  /**
   * Gets the the {@code frequency} property.
   * @return the property, not null
   */
  public final Property<Frequency> frequency() {
    return metaBean().frequency().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the region external identifier.
   * @return the value of the property, not null
   */
  public ExternalId getRegionIdentifier() {
    return _regionIdentifier;
  }

  /**
   * Sets the region external identifier.
   * @param regionIdentifier  the new value of the property, not null
   */
  public void setRegionIdentifier(ExternalId regionIdentifier) {
    JodaBeanUtils.notNull(regionIdentifier, "regionIdentifier");
    this._regionIdentifier = regionIdentifier;
  }

  /**
   * Gets the the {@code regionIdentifier} property.
   * @return the property, not null
   */
  public final Property<ExternalId> regionIdentifier() {
    return metaBean().regionIdentifier().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the business day convention.
   * @return the value of the property, not null
   */
  public BusinessDayConvention getBusinessDayConvention() {
    return _businessDayConvention;
  }

  /**
   * Sets the business day convention.
   * @param businessDayConvention  the new value of the property, not null
   */
  public void setBusinessDayConvention(BusinessDayConvention businessDayConvention) {
    JodaBeanUtils.notNull(businessDayConvention, "businessDayConvention");
    this._businessDayConvention = businessDayConvention;
  }

  /**
   * Gets the the {@code businessDayConvention} property.
   * @return the property, not null
   */
  public final Property<BusinessDayConvention> businessDayConvention() {
    return metaBean().businessDayConvention().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional.
   * @return the value of the property, not null
   */
  public Notional getNotional() {
    return _notional;
  }

  /**
   * Sets the notional.
   * @param notional  the new value of the property, not null
   */
  public void setNotional(Notional notional) {
    JodaBeanUtils.notNull(notional, "notional");
    this._notional = notional;
  }

  /**
   * Gets the the {@code notional} property.
   * @return the property, not null
   */
  public final Property<Notional> notional() {
    return metaBean().notional().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwapLeg}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> _dayCount = DirectMetaProperty.ofReadWrite(
        this, "dayCount", SwapLeg.class, DayCount.class);
    /**
     * The meta-property for the {@code frequency} property.
     */
    private final MetaProperty<Frequency> _frequency = DirectMetaProperty.ofReadWrite(
        this, "frequency", SwapLeg.class, Frequency.class);
    /**
     * The meta-property for the {@code regionIdentifier} property.
     */
    private final MetaProperty<ExternalId> _regionIdentifier = DirectMetaProperty.ofReadWrite(
        this, "regionIdentifier", SwapLeg.class, ExternalId.class);
    /**
     * The meta-property for the {@code businessDayConvention} property.
     */
    private final MetaProperty<BusinessDayConvention> _businessDayConvention = DirectMetaProperty.ofReadWrite(
        this, "businessDayConvention", SwapLeg.class, BusinessDayConvention.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Notional> _notional = DirectMetaProperty.ofReadWrite(
        this, "notional", SwapLeg.class, Notional.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
        this, null,
        "dayCount",
        "frequency",
        "regionIdentifier",
        "businessDayConvention",
        "notional");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1905311443:  // dayCount
          return _dayCount;
        case -70023844:  // frequency
          return _frequency;
        case 19238589:  // regionIdentifier
          return _regionIdentifier;
        case -1002835891:  // businessDayConvention
          return _businessDayConvention;
        case 1585636160:  // notional
          return _notional;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SwapLeg> builder() {
      throw new UnsupportedOperationException("SwapLeg is an abstract class");
    }

    @Override
    public Class<? extends SwapLeg> beanType() {
      return SwapLeg.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<DayCount> dayCount() {
      return _dayCount;
    }

    /**
     * The meta-property for the {@code frequency} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Frequency> frequency() {
      return _frequency;
    }

    /**
     * The meta-property for the {@code regionIdentifier} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ExternalId> regionIdentifier() {
      return _regionIdentifier;
    }

    /**
     * The meta-property for the {@code businessDayConvention} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<BusinessDayConvention> businessDayConvention() {
      return _businessDayConvention;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<Notional> notional() {
      return _notional;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
