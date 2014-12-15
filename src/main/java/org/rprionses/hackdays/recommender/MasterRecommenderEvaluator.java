/* Copyright 2014 Bronto Software, Inc. */
package org.rprionses.hackdays.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.SpearmanCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.rprionses.hackdays.recommender.SimpleUserBasedRecommenderEvaluator.SimpleRecommenderBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MasterRecommenderEvaluator {

  private final List<RecommenderBuilder> builder;
  private final RecommenderEvaluator evaluator;
  private final DataModel dataModel;

  public MasterRecommenderEvaluator(RecommenderEvaluator evaluator, DataModel dataModel,
      List<RecommenderBuilder> similarities) throws Exception {

    this.evaluator = evaluator;
    this.dataModel = dataModel;
    this.builder = similarities;
  }

  public void evaluate() throws Exception {

    for (RecommenderBuilder builder : builder) {

      long t1 = System.currentTimeMillis();
      double result = evaluator.evaluate(builder, null, dataModel, 0.9, 1.0);
      long t2 = System.currentTimeMillis();
      System.out.println(builder.getSimilarityType() + ": " + result + " (" + (t2 - t1) + "ms)");
    }
  }

  public interface RecommenderBuilder extends org.apache.mahout.cf.taste.eval.RecommenderBuilder {

    String getSimilarityType();

    Recommender buildRecommender(DataModel dataModel) throws TasteException;
  }

  public static void main(String[] args) throws Exception {

    DataModel dataModel = new FileDataModel(new File("src/main/resources/data/movies.csv"));

    List<RecommenderBuilder> builders = new ArrayList<RecommenderBuilder>();
    builders.add(new SimpleRecommenderBuilder(new EuclideanDistanceSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new PearsonCorrelationSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new UncenteredCosineSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new CityBlockSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new LogLikelihoodSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new SpearmanCorrelationSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new TanimotoCoefficientSimilarity(dataModel)));

    RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();

    new MasterRecommenderEvaluator(evaluator, dataModel, builders).evaluate();
  }
}
