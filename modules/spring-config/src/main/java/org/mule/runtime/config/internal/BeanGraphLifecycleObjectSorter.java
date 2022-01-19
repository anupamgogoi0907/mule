/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.reverse;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.Lists.newArrayList;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.lifecycle.phases.LifecycleObjectSorter;

import java.util.ArrayList;
import java.util.List;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

public class BeanGraphLifecycleObjectSorter implements LifecycleObjectSorter {

  private List<DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge>> dependencyGraphs;
  private List<CycleDetector> cycleDetectors;
  private BeanGraphBeanDependencyResolver resolver;
  protected Class<?>[] orderedLifecycleTypes;

  public BeanGraphLifecycleObjectSorter(BeanGraphBeanDependencyResolver resolver, Class<?>[] orderedLifecycleTypes) {
    this.dependencyGraphs = new ArrayList<>();
    this.cycleDetectors = new ArrayList<>();
    this.resolver = resolver;
    this.orderedLifecycleTypes = orderedLifecycleTypes;
    for (int i = 0; i < orderedLifecycleTypes.length; i++) {
      DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> graph = new DefaultDirectedGraph(DefaultEdge.class);
      dependencyGraphs.add(graph);
      cycleDetectors.add(new CycleDetector(graph));
    }
  }

  // building a single dependency graph for each lifecycle type during initialization phase
  @Override
  public void addObject(String beanName, Object currentObject) {

    requireNonNull(currentObject, "currentObject cannot be null");

    if (stream(orderedLifecycleTypes).noneMatch(x -> x.isInstance(currentObject))) {
      return;
    }

    DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> dependencyGraph = getDependencyGraphForLifecycleType(currentObject);
    CycleDetector cycleDetector = getCycleDetector(currentObject);

    BeanVertexWrapper currentVertex = new BeanVertexWrapper(beanName, currentObject);
    dependencyGraph.addVertex(currentVertex);

    // get (direct) prerequisite objects for the current object
    List<Pair<String, Object>> prerequisiteObjects = resolver.getDirectBeanDependencies(beanName);

    // add direct prerequisites to the graph & edges
    if (prerequisiteObjects.isEmpty()) {
      return;
    }
    prerequisiteObjects.forEach(
                                (prerequisite) -> {
                                  String preReqName = prerequisite.getFirst();
                                  Object preReqObject = prerequisite.getSecond();

                                  BeanVertexWrapper preReqVertex = new BeanVertexWrapper(preReqName, preReqObject);
                                  dependencyGraph.addVertex(preReqVertex);

                                  // todo: update the cycle check part below, cyclecheck after adding edge
                                  if (!dependencyGraph.containsEdge(preReqVertex, currentVertex)) {
                                    dependencyGraph.addEdge(currentVertex, preReqVertex);
                                    if (cycleDetector.detectCycles()) {
                                      dependencyGraph.removeEdge(currentVertex, preReqVertex);
                                    }
                                  }
                                });
  }

  private int getDependencyGraphIndex(Object currentObject) {
    Class<?> clazz = stream(orderedLifecycleTypes).filter(x -> x.isInstance(currentObject)).findFirst().get();
    return asList(orderedLifecycleTypes).indexOf(clazz);
  }

  private DefaultDirectedGraph<BeanVertexWrapper, DefaultEdge> getDependencyGraphForLifecycleType(Object currentObject) {
    return dependencyGraphs.get(getDependencyGraphIndex(currentObject));
  }

  private CycleDetector getCycleDetector(Object currentObject) {
    return cycleDetectors.get(getDependencyGraphIndex(currentObject));
  }

  @Override
  public List<Object> getSortedObjects() {
    return dependencyGraphs.stream().map(x -> {
      List<BeanVertexWrapper> sortedObjects = newArrayList(new TopologicalOrderIterator<>(x));
      reverse(sortedObjects);
      return sortedObjects;
    }).reduce(new ArrayList<>(), (sortedObjectList, b) -> {
      for (BeanVertexWrapper v : b) {
        if (!sortedObjectList.contains(v)) {
          sortedObjectList.add(v);
        }
      }
      return sortedObjectList;
    }).stream().map(BeanVertexWrapper::getWrappedObject).collect(toList());
  }


}
