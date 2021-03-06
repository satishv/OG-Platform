/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.engine.fudgemsg;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.fudgemsg.FudgeField;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.MutableFudgeMsg;
import org.fudgemsg.mapping.FudgeBuilder;
import org.fudgemsg.mapping.FudgeBuilderFor;
import org.fudgemsg.mapping.FudgeDeserializer;
import org.fudgemsg.mapping.FudgeSerializer;

import com.opengamma.engine.view.cache.CacheSelectHint;
import com.opengamma.engine.view.calcnode.CalculationJob;
import com.opengamma.engine.view.calcnode.CalculationJobItem;
import com.opengamma.engine.view.calcnode.CalculationJobSpecification;

/**
 * Fudge message builder for {@code CalculationJob}.
 */
@FudgeBuilderFor(CalculationJob.class)
public class CalculationJobFudgeBuilder implements FudgeBuilder<CalculationJob> {
  private static final String REQUIRED_FIELD_NAME = "requiredJobId";
  private static final String FUNCTION_INITIALIZATION_IDENTIFIER_FIELD_NAME = "functionInitId";
  private static final String ITEM_FIELD_NAME = "calculationJobItem";

  @Override
  public MutableFudgeMsg buildMessage(FudgeSerializer serializer, CalculationJob object) {
    MutableFudgeMsg msg = serializer.objectToFudgeMsg(object.getSpecification());
    msg.add(FUNCTION_INITIALIZATION_IDENTIFIER_FIELD_NAME, object.getFunctionInitializationIdentifier());
    if (object.getRequiredJobIds() != null) {
      for (Long required : object.getRequiredJobIds()) {
        msg.add(REQUIRED_FIELD_NAME, required);
      }
    }
    for (CalculationJobItem item : object.getJobItems()) {
      serializer.addToMessage(msg, ITEM_FIELD_NAME, null, item);
    }
    MutableFudgeMsg cacheSelectHintMsg = serializer.objectToFudgeMsg(object.getCacheSelectHint());
    for (FudgeField fudgeField : cacheSelectHintMsg.getAllFields()) {
      msg.add(fudgeField);
    }
    return msg;
  }

  @Override
  public CalculationJob buildObject(FudgeDeserializer deserializer, FudgeMsg message) {
    CalculationJobSpecification jobSpec = deserializer.fudgeMsgToObject(CalculationJobSpecification.class, message);
    Validate.notNull(jobSpec, "Fudge message is not a CalculationJob - field 'calculationJobSpecification' is not present");
    final long functionInitializationIdentifier = message.getLong(FUNCTION_INITIALIZATION_IDENTIFIER_FIELD_NAME);
    Collection<FudgeField> fields = message.getAllByName(REQUIRED_FIELD_NAME);
    ArrayList<Long> requiredJobIds = null;
    if (!fields.isEmpty()) {
      requiredJobIds = new ArrayList<Long>(fields.size());
      for (FudgeField field : fields) {
        requiredJobIds.add(((Number) field.getValue()).longValue());
      }
    }
    fields = message.getAllByName(ITEM_FIELD_NAME);
    final ArrayList<CalculationJobItem> jobItems = new ArrayList<CalculationJobItem>(fields.size());
    for (FudgeField field : fields) {
      CalculationJobItem jobItem = deserializer.fudgeMsgToObject(CalculationJobItem.class, (FudgeMsg) field.getValue());
      jobItems.add(jobItem);
    }
    CacheSelectHint cacheSelectFilter = deserializer.fudgeMsgToObject(CacheSelectHint.class, message);
    return new CalculationJob(jobSpec, functionInitializationIdentifier, requiredJobIds, jobItems, cacheSelectFilter);
  }

}
