/* Copyright 2014 Bronto Software, Inc. */
package org.rprionses.hackdays.recommender;

import org.apache.commons.io.FileUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.BooleanPreference;
import org.apache.mahout.cf.taste.impl.model.BooleanUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.model.GenericBooleanPrefDataModel;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericBooleanPrefUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.LogLikelihoodSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.rprionses.hackdays.orders.client.SimpleOrderClient;
import org.rprionses.hackdays.orders.client.SimpleOrderClient.SimpleOrder;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleUserBasedRecommender {

  private final Recommender recommender;

  public SimpleUserBasedRecommender(DataModel dataModel) throws Exception {

    // build the recommender with any similarity you like
    UserSimilarity similarity = new LogLikelihoodSimilarity(dataModel);
    // UserSimilarity similarity = new EuclideanDistanceSimilarity(dataModel);
    // UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    // UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
    // UserSimilarity similarity = new CityBlockSimilarity(dataModel);
    // UserSimilarity similarity = new TanimotoCoefficientSimilarity(dataModel);

    SimpleRecommenderBuilder builder = new SimpleRecommenderBuilder(similarity);
    recommender = builder.buildRecommender(dataModel);
  }

  public void outputAllRecommendations(int numberOfRecommendations) throws Exception {

    LongPrimitiveIterator userIDs = recommender.getDataModel().getUserIDs();
    while (userIDs.hasNext()) {

      Long userId = userIDs.next();
      List<RecommendedItem> recommendedItems = recommender.recommend(userId, numberOfRecommendations);

      if (!recommendedItems.isEmpty()) {
        for (RecommendedItem recommendedItem : recommendedItems) {
          System.out.println(" " + userId + ", " + recommendedItem.getItemID() + ", " + recommendedItem.getValue());
        }
      } else {
        System.out.println(" " + String.format("No recommendations found for %d", userId));
      }
    }
  }

  static class SimpleRecommenderBuilder implements RecommenderBuilder {

    private final UserSimilarity similarity;

    public SimpleRecommenderBuilder(UserSimilarity similarity) {
      this.similarity = similarity;
    }

    public Recommender buildRecommender(DataModel dataModel) throws TasteException {

      // build the neighborhood
      UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, dataModel);

      // build the recommender, being sure to use the boolean preference based recommender
      UserBasedRecommender recommender = new GenericBooleanPrefUserBasedRecommender(dataModel, neighborhood, similarity);

      return recommender;
    }

    public String getType() {
      return similarity.getClass().getSimpleName();
    }
  }

  public static void main(String[] args) throws Exception {

    supressStandardError();

    // first, run the recommender using the OS data

    // get the orders from OS
    SimpleOrderClient client = SimpleOrderClient.createClient();
    List<SimpleOrder> simpleOrders = client.getAllSimpleOrders(1, 100);

    System.out.println("\nOS data:");

    // map the orders to a form the the data model can use
    Map<Long, List<Preference>> preferenceMap = new HashMap<Long, List<Preference>>();
    for (SimpleOrder simpleOrder : simpleOrders) {

      System.out.println(" " + simpleOrder.toStringForMahout());
      long userId = simpleOrder.getUserId();
      List<Preference> preferences = preferenceMap.get(userId);
      if (preferences == null) {
        preferences = new ArrayList<Preference>();
        preferenceMap.put(userId, preferences);
      }

      Preference preference = new BooleanPreference(userId, simpleOrder.getItemId());
      preferences.add(preference);
    }

    FastByIDMap<PreferenceArray> dataModelMap = new FastByIDMap<PreferenceArray>();
    for (Entry<Long, List<Preference>> entry : preferenceMap.entrySet()) {
      dataModelMap.put(entry.getKey(), new BooleanUserPreferenceArray(entry.getValue()));
    }

    // create the data model from the map
    GenericBooleanPrefDataModel osDataModel = new GenericBooleanPrefDataModel(
        GenericBooleanPrefDataModel.toDataMap(dataModelMap));

    // create and run the recommender
    SimpleUserBasedRecommender osDataRecommender = new SimpleUserBasedRecommender(osDataModel);

    System.out.println("\nOS data recommendations:");
    osDataRecommender.outputAllRecommendations(2);

    // then, run the recommender using the file data

    System.out.println("\nFile data:");

    File dataFile = new File("src/main/resources/data/testdata.csv");
    List<String> lines = FileUtils.readLines(dataFile);
    for (String line : lines) {
      System.out.println(" " + line);
    }

    System.out.println("\nFile data recommendations:");

    FileDataModel fileDataModel = new FileDataModel(dataFile);
    SimpleUserBasedRecommender fileBasedRecommender = new SimpleUserBasedRecommender(fileDataModel);
    fileBasedRecommender.outputAllRecommendations(2);
    ;
  }

  static void supressStandardError() {
    System.setErr(new PrintStream(new OutputStream() {
      public void write(int b) throws IOException {
      }
    }));
  }
}
