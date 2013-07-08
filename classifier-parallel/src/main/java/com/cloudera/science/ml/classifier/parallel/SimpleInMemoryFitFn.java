/**
 * Copyright (c) 2013, Cloudera, Inc. All Rights Reserved.
 *
 * Cloudera, Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package com.cloudera.science.ml.classifier.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.crunch.Emitter;
import org.apache.crunch.Pair;

import com.cloudera.science.ml.classifier.core.OnlineLearnerRun;
import com.cloudera.science.ml.classifier.simple.SimpleOnlineLearner;
import com.cloudera.science.ml.core.vectors.LabeledVector;
import com.cloudera.science.ml.parallel.base.Pairs;

/**
 * A fit function for SimpleOnlineLearners that reads all the training data
 * and samples randomly from it instead of streaming over it once.
 */
public class SimpleInMemoryFitFn extends FitFn {
  private List<SimpleOnlineLearner> learners;
  private long seed;
  private int numIters;
  
  public SimpleInMemoryFitFn(List<SimpleOnlineLearner> learners, long seed,
      int numIters) {
    this.learners = learners;
    this.seed = seed;
    this.numIters = numIters;
  }
  
  @Override
  public void process(Pair<Pair<Integer, Integer>, Iterable<Pair<Integer, LabeledVector>>> in,
      Emitter<OnlineLearnerRun> emitter) {
    Random rand = new Random(seed + in.first().hashCode());
    List<LabeledVector> observations = new ArrayList<LabeledVector>();
    for (LabeledVector obs : Pairs.second(in.second())) {
      observations.add(obs);
    }

    for (int i = 0; i < numIters; i++) {
      for (SimpleOnlineLearner learner : learners) {
        learner.update(observations.get(rand.nextInt(observations.size())));
      }
    }

    for (int i = 0; i < learners.size(); i++) {
      SimpleOnlineLearner learner = learners.get(i);
      int fold = in.first().first();
      int partition = in.first().second();
      emitter.emit(new OnlineLearnerRun(learner.getClassifier(),
          learner.getClass(), learner.getParams(), fold, partition, i));
    }
  } 

}