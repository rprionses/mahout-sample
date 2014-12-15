/* Copyright 2014 Bronto Software, Inc. */
package org.rprionses.hackdays.recommender;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.RMSRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.CityBlockSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.EuclideanDistanceSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.TanimotoCoefficientSimilarity;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SimpleItemBasedRecommenderEvaluator extends MasterRecommenderEvaluator {

  public SimpleItemBasedRecommenderEvaluator(RecommenderEvaluator evaluator, DataModel dataModel,
      List<RecommenderBuilder> builders) throws Exception {
    super(evaluator, dataModel, builders);
  }

  static class SimpleRecommenderBuilder implements RecommenderBuilder {

    private final ItemSimilarity similarity;

    public SimpleRecommenderBuilder(ItemSimilarity similarity) {
      this.similarity = similarity;
    }

    public Recommender buildRecommender(DataModel dataModel) throws TasteException {
      return new GenericItemBasedRecommender(dataModel, similarity);
    }

    public String getSimilarityType() {
      return similarity.getClass().getSimpleName();
    }
  }

  public static void main(String[] args) throws Exception {

    supressStandardError();

    DataModel dataModel = new FileDataModel(new File("src/main/resources/data/movies.csv"));

    List<RecommenderBuilder> builders = new ArrayList<RecommenderBuilder>();
    builders.add(new SimpleRecommenderBuilder(new EuclideanDistanceSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new PearsonCorrelationSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new UncenteredCosineSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new CityBlockSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new LogLikelihoodSimilarity(dataModel)));
    builders.add(new SimpleRecommenderBuilder(new TanimotoCoefficientSimilarity(dataModel)));

    RecommenderEvaluator recommenderEvaluator = new RMSRecommenderEvaluator();
    SimpleItemBasedRecommenderEvaluator itemBasedRecommenderEvaluator = new SimpleItemBasedRecommenderEvaluator(
        recommenderEvaluator, dataModel, builders);
    itemBasedRecommenderEvaluator.evaluate();
  }

  static void supressStandardError() {
    System.setErr(new PrintStream(new OutputStream() {
      public void write(int b) throws IOException {
      }
    }));
  }

}
