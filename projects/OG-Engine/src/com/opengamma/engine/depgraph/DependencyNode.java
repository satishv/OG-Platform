/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.engine.depgraph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.opengamma.core.security.Security;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.function.CompiledFunctionDefinition;
import com.opengamma.engine.function.MarketDataSourcingFunction;
import com.opengamma.engine.function.ParameterizedFunction;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.PublicAPI;
import com.opengamma.util.tuple.Pair;

/**
 * A single node in a {@link DependencyGraph}. A node represents the need to execute a particular function at
 * runtime to produce certain outputs.
 * <p>
 * The same node instance can belong to multiple graphs due to the possibility of sub-graphing.
 * <p>
 * A node consists of a computation target (e.g. a {@link Security}), a function to operate on that target, input
 * values for the function, and output values generated by the function. Relationships with other nodes - either input
 * or dependent - indicate how input values are produced and how output values are to be used.
 */
@PublicAPI
public class DependencyNode {

  // BELOW: COMPLETELY IMMUTABLE VARIABLES

  private final ComputationTarget _computationTarget;

  // COMPLETELY IMMUTABLE VARIABLES END

  private ParameterizedFunction _function;

  // BELOW: EVEN THOUGH VARIABLE ITSELF IS FINAL, CONTENTS ARE MUTABLE.

  private final Set<ValueSpecification> _inputValues = new HashSet<ValueSpecification>();
  private final Set<ValueSpecification> _outputValues = new HashSet<ValueSpecification>();

  private final Set<DependencyNode> _inputNodes = new HashSet<DependencyNode>();
  private final Set<DependencyNode> _dependentNodes = new HashSet<DependencyNode>();

  /**
   * The final output values that cannot be stripped from the {@link #_outputValues} set no matter
   * whether there are no dependent nodes.
   */
  private final Set<ValueSpecification> _terminalOutputValues = new HashSet<ValueSpecification>();

  // MUTABLE CONTENTS VARIABLES END

  /**
   * Creates a new node.
   * 
   * @param target the computation target, not null
   */
  public DependencyNode(ComputationTarget target) {
    ArgumentChecker.notNull(target, "Computation Target");
    _computationTarget = target;
  }

  /**
   * Adds a set of nodes as inputs to this node. The nodes added are updated to include this node in their
   * dependent node set.
   * 
   * @param inputNodes nodes to add, not null and not containing null
   */
  public void addInputNodes(Set<DependencyNode> inputNodes) {
    for (DependencyNode inputNode : inputNodes) {
      addInputNode(inputNode);
    }
  }

  /**
   * Adds a node as input to this node. The node added is updated to include this node in its dependent node
   * set.
   * 
   * @param inputNode node to add, not null
   */
  public void addInputNode(DependencyNode inputNode) {
    ArgumentChecker.notNull(inputNode, "Input Node");
    _inputNodes.add(inputNode);
    inputNode.addDependentNode(this);
  }

  protected void addDependentNode(DependencyNode dependentNode) {
    ArgumentChecker.notNull(dependentNode, "Dependent Node");
    _dependentNodes.add(dependentNode);
  }

  /**
   * Returns the set of all immediately dependent nodes - i.e. nodes that consume one or more output values generated
   * by this node.
   * 
   * @return the set of dependent nodes
   */
  public Set<DependencyNode> getDependentNodes() {
    return Collections.unmodifiableSet(_dependentNodes);
  }

  /**
   * Returns the dependent node. This call is only valid if there is a single (or no) dependent node.
   * 
   * @return the node, or null if there are no dependent nodes
   * @throws IllegalStateException if there are multiple dependent nodes
   */
  public DependencyNode getDependentNode() {
    if (_dependentNodes.isEmpty()) {
      return null;
    } else if (_dependentNodes.size() > 1) {
      throw new IllegalStateException("More than one dependent node");
    } else {
      return _dependentNodes.iterator().next();
    }
  }

  /**
   * Returns the set of all immediate input nodes - i.e. nodes that produce one or more of the input values to the function
   * attached to this node.
   * 
   * @return the set of input nodes
   */
  public Set<DependencyNode> getInputNodes() {
    return Collections.unmodifiableSet(_inputNodes);
  }

  /**
   * Adds output values to this node. Graph construction will initially include the maximal set of outputs from the function.
   * This will later be pruned to remove any values not required as inputs to other nodes and not specified as terminal outputs
   * of the graph.
   * 
   * @param outputValues the output values produced by this node, not null
   */
  public void addOutputValues(Set<ValueSpecification> outputValues) {
    for (ValueSpecification outputValue : outputValues) {
      addOutputValue(outputValue);
    }
  }

  /**
   * Adds an output value to the node. Graph construction will initially include the maximal set of outputs from the function.
   * This will later be pruned to remove any values not required as inputs to other nodes and not specified as terminal outputs
   * of the graph.
   * 
   * @param outputValue an output value produced by this node, not null
   */
  public void addOutputValue(ValueSpecification outputValue) {
    ArgumentChecker.notNull(outputValue, "Output value");
    _outputValues.add(outputValue);
  }

  /**
   * Removes an output value from this node. The value must not be used as an input to a dependent node.
   * 
   * @param outputValue the value to remove
   */
  public void removeOutputValue(ValueSpecification outputValue) {
    for (DependencyNode outputNode : _dependentNodes) {
      if (outputNode._inputValues.contains(outputValue)) {
        throw new IllegalStateException("Can't remove output value " + outputValue + " required for input to " + outputNode);
      }
    }
    if (!_outputValues.remove(outputValue)) {
      throw new IllegalStateException("Output value " + outputValue + " not in output set of " + this);
    }
  }

  /**
   * Replace an output value from this node with another.
   * 
   * @param existingOutputValue the existing value
   * @param newOutputValue the value to replace it with
   */
  public void replaceOutputValue(final ValueSpecification existingOutputValue, final ValueSpecification newOutputValue) {
    if (!_outputValues.remove(existingOutputValue)) {
      throw new IllegalStateException("Existing output value " + existingOutputValue + " not in output set of " + this);
    }
    _outputValues.add(newOutputValue);
    for (DependencyNode outputNode : _dependentNodes) {
      if (outputNode._inputValues.remove(existingOutputValue)) {
        outputNode._inputValues.add(newOutputValue);
      }
    }
  }

  /* package */void clearOutputValues() {
    _outputValues.clear();
  }

  /* package */void clearInputs() {
    for (DependencyNode inputNode : _inputNodes) {
      inputNode._dependentNodes.remove(this);
    }
    _inputNodes.clear();
    _inputValues.clear();
  }

  public void addInputValue(ValueSpecification inputValue) {
    ArgumentChecker.notNull(inputValue, "Input value");
    _inputValues.add(inputValue);
  }

  /**
   * Returns the set of output values produced by this node.
   * 
   * @return the set of output values
   */
  public Set<ValueSpecification> getOutputValues() {
    return Collections.unmodifiableSet(_outputValues);
  }

  /**
   * Returns the set of terminal output values produced by this node. This is a subset of {@link #getOutputValues}. After
   * graph construction any output values that are not consumed by other nodes will be pruned unless they are declared
   * as terminal output values.
   * 
   * @return the set of output values, or the empty set if none
   */
  public Set<ValueSpecification> getTerminalOutputValues() {
    return Collections.unmodifiableSet(_terminalOutputValues);
  }

  /**
   * Returns the set of output values expressed as {@link ValueRequirement} instances.
   * 
   * @return the set of output values
   */
  public Set<ValueRequirement> getOutputRequirements() {
    Set<ValueRequirement> outputRequirements = new HashSet<ValueRequirement>();
    for (ValueSpecification outputValue : _outputValues) {
      outputRequirements.add(outputValue.toRequirementSpecification());
    }
    return outputRequirements;
  }

  /**
   * Returns the set of input values.
   * 
   * @return the set of input values
   */
  public Set<ValueSpecification> getInputValues() {
    return Collections.unmodifiableSet(_inputValues);
  }

  /**
   * Tests if a given value is an input to this node.
   * 
   * @param specification value to test
   * @return true if the value is an input to this node
   */
  public boolean hasInputValue(final ValueSpecification specification) {
    return _inputValues.contains(specification);
  }

  /**
   * Returns the market data requirement of this node.
   * 
   * @return the market data requirement.
   */
  public Pair<ValueRequirement, ValueSpecification> getRequiredMarketData() {
    if (_function.getFunction() instanceof MarketDataSourcingFunction) {
      final MarketDataSourcingFunction ldsf = ((MarketDataSourcingFunction) _function.getFunction());
      return ldsf.getMarketDataRequirement();
    }
    return null;
  }

  /**
   * Returns the function used at this node.
   * 
   * @return the function
   */
  public ParameterizedFunction getFunction() {
    return _function;
  }

  /**
   * Uses default parameters to invoke the function. Useful in tests.
   * 
   * @param function Function to be invoked
   */
  public void setFunction(CompiledFunctionDefinition function) {
    setFunction(new ParameterizedFunction(function, function.getFunctionDefinition().getDefaultParameters()));
  }

  /**
   * Sets the function to be used to execute this node.
   * 
   * @param function Function to be invoked
   */
  public void setFunction(ParameterizedFunction function) {
    ArgumentChecker.notNull(function, "Function");
    if (_function != null) {
      throw new IllegalStateException("The function was already set");
    }

    if (function.getFunction().getTargetType() != getComputationTarget().getType()) {
      throw new IllegalArgumentException("Provided function of type " + function.getFunction().getTargetType() + " but target of type " + getComputationTarget().getType());
    }

    _function = function;
  }

  /**
   * Returns the computation target of the node.
   * 
   * @return the computation target
   */
  public ComputationTarget getComputationTarget() {
    return _computationTarget;
  }

  /**
   * Removes any unused outputs. These are any output values that are not terminal output values and are not
   * stated as inputs to any dependent nodes.
   * 
   * @return the set of outputs removed, or the empty set if none were removed
   */
  public Set<ValueSpecification> removeUnnecessaryOutputs() {
    Set<ValueSpecification> unnecessaryOutputs = new HashSet<ValueSpecification>();
    for (ValueSpecification outputSpec : _outputValues) {
      if (_terminalOutputValues.contains(outputSpec)) {
        continue;
      }
      boolean isUsed = false;
      for (DependencyNode dependantNode : _dependentNodes) {
        if (dependantNode.hasInputValue(outputSpec)) {
          isUsed = true;
          break;
        }
      }
      if (!isUsed) {
        unnecessaryOutputs.add(outputSpec);
      }
    }
    _outputValues.removeAll(unnecessaryOutputs);
    return unnecessaryOutputs;
  }

  /**
   * Marks an output as terminal, meaning that it cannot be pruned. If this node already belongs to a graph, use
   * {@link DependencyGraph#addTerminalOutput(ValueRequirement requirement, ValueSpecification specification)}.
   * 
   * @param terminalOutput  the output to mark as terminal
   */
  public void addTerminalOutputValue(ValueSpecification terminalOutput) {
    _terminalOutputValues.add(terminalOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("DependencyNode[");
    if (getFunction() != null) {
      sb.append(getFunction().getFunction().getFunctionDefinition().getShortName());
    } else {
      sb.append("<null function>");
    }
    sb.append(" on ");
    sb.append(getComputationTarget().toSpecification());
    sb.append("]");
    return sb.toString();
  }

}
