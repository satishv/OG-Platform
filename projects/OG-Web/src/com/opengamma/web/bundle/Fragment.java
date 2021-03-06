/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.bundle;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.util.ArgumentChecker;

/**
 * A fragment of a bundle, representing a single CSS/Javascript file.
 * <p>
 * Fragments are not referred to directly, instead they are grouped into
 * {@link Bundle bundles}.
 * <p>
 * This class is mutable and not thread-safe.
 */
@BeanDefinition
public class Fragment extends DirectBean implements BundleNode {

  /**
   * The file representation
   */
  @PropertyDefinition
  private File _file;

  /**
   * Creates an instance.
   */
  protected Fragment() {
  }

  /**
   * Creates an instance for a file.
   * 
   * @param file  the file, not null
   */
  public Fragment(File file) {
    ArgumentChecker.notNull(file, "file");
    _file = file;
  }

  //-------------------------------------------------------------------------
  @Override
  public List<Bundle> getAllBundles() {
    return Collections.emptyList();
  }

  @Override
  public List<Fragment> getAllFragments() {
    return Collections.singletonList(this);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Fragment}.
   * @return the meta-bean, not null
   */
  public static Fragment.Meta meta() {
    return Fragment.Meta.INSTANCE;
  }
  static {
    JodaBeanUtils.registerMetaBean(Fragment.Meta.INSTANCE);
  }

  @Override
  public Fragment.Meta metaBean() {
    return Fragment.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3143036:  // file
        return getFile();
    }
    return super.propertyGet(propertyName, quiet);
  }

  @Override
  protected void propertySet(String propertyName, Object newValue, boolean quiet) {
    switch (propertyName.hashCode()) {
      case 3143036:  // file
        setFile((File) newValue);
        return;
    }
    super.propertySet(propertyName, newValue, quiet);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Fragment other = (Fragment) obj;
      return JodaBeanUtils.equal(getFile(), other.getFile());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getFile());
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the file representation
   * @return the value of the property
   */
  public File getFile() {
    return _file;
  }

  /**
   * Sets the file representation
   * @param file  the new value of the property
   */
  public void setFile(File file) {
    this._file = file;
  }

  /**
   * Gets the the {@code file} property.
   * @return the property, not null
   */
  public final Property<File> file() {
    return metaBean().file().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Fragment}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code file} property.
     */
    private final MetaProperty<File> _file = DirectMetaProperty.ofReadWrite(
        this, "file", Fragment.class, File.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map = new DirectMetaPropertyMap(
        this, null,
        "file");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3143036:  // file
          return _file;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Fragment> builder() {
      return new DirectBeanBuilder<Fragment>(new Fragment());
    }

    @Override
    public Class<? extends Fragment> beanType() {
      return Fragment.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code file} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<File> file() {
      return _file;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
