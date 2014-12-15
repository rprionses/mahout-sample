/* Copyright 2014 Bronto Software, Inc. */
package org.rprionses.hackdays.orders.client;

import com.bronto.chunk.Messages;
import com.bronto.message.payload.order.LineItem;
import com.bronto.message.payload.order.LineItemBuilder;
import com.bronto.message.payload.order.Order;
import com.bronto.orders.client.OrderAddRequestBuilder;
import com.bronto.orders.client.OrderSearchBuilder;
import com.bronto.orders.client.OrderServiceClient;
import com.bronto.orders.client.OrderServiceClientBuilder;
import com.bronto.service.test.TestHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SimpleOrderClient {

  private final OrderServiceClient client;
  private final Map<String, Long> skuMap;
  private final Set<Long> contactIds;

  public static SimpleOrderClient createClient() throws Exception {
    return new SimpleOrderClient("ross.brontolabs.local", 9100, "http", "ross.brontolabs.local", "orders");
  }

  public SimpleOrderClient(String hostname, int port, String protocol, String xReqHost, String xReqName)
      throws Exception {

    OrderServiceClientBuilder builder = OrderServiceClientBuilder.newInstance();
    builder.withHostname(hostname);
    builder.withPort(port);
    builder.withProtocol(protocol);
    builder.withXReqHost(xReqHost);
    builder.withXReqName(xReqName);

    this.client = builder.build();
    this.skuMap = new HashMap<String, Long>();
    this.contactIds = new HashSet<Long>();
  }

  public List<Order> getOrders(long siteId, long contactId) {

    OrderSearchBuilder orderSearchBuilder = client.searchOrders(siteId);
    orderSearchBuilder.contactId(contactId);

    return orderSearchBuilder.search();
  }

  public List<SimpleOrder> getAllSimpleOrders(long siteId) {

    List<SimpleOrder> simpleOrders = new ArrayList<SimpleOrder>();
    for (Long contactId : contactIds) {
      simpleOrders.addAll(getSimpleOrders(siteId, contactId));
    }

    return simpleOrders;
  }

  public List<SimpleOrder> getAllSimpleOrders(long siteId, long lastContactId) {

    List<SimpleOrder> simpleOrders = new ArrayList<SimpleOrder>();
    for (int contactId = 1; contactId <= lastContactId; contactId++) {
      simpleOrders.addAll(getSimpleOrders(siteId, contactId));
    }

    return simpleOrders;
  }

  public List<SimpleOrder> getSimpleOrders(long siteId, long contactId) {

    List<SimpleOrder> simpleOrders = new ArrayList<SimpleOrder>();

    OrderSearchBuilder orderSearchBuilder = client.searchOrders(siteId);
    orderSearchBuilder.contactId(contactId);

    Long orderId = 0L;

    List<Order> orders = orderSearchBuilder.search();
    for (Order order : orders) {

      List<LineItem> lineItems = order.getLineItems();
      for (LineItem lineItem : lineItems) {

        String sku = lineItem.getSku();
        orderId = skuMap.get(sku);
        if (orderId == null) {
          orderId = skuMap.size() + 1L;
          skuMap.put(sku, orderId);
        }

        BigDecimal quantity = lineItem.getQuantity();
        boolean ordered = quantity != null && quantity.intValue() > 0;
        simpleOrders.add(SimpleOrder.createOrder(contactId, orderId, ordered));
      }
    }

    return simpleOrders;
  }

  public Order addOrder(long siteId, long contactId, String sku) {

    contactIds.add(contactId);

    System.out.println(String.format("Adding item %s for contact %d", sku, contactId));

    LineItemBuilder lineItemBuilder = Messages.newBuilder(LineItemBuilder.class);
    lineItemBuilder.sku(sku);
    lineItemBuilder.quantity(BigDecimal.valueOf(4));
    lineItemBuilder.totalPrice(BigDecimal.valueOf(40));

    OrderAddRequestBuilder orderBuilder = client.addOrder(siteId);
    orderBuilder.contactId(contactId);
    orderBuilder.lineItem(lineItemBuilder.build());
    orderBuilder.cartId(UUID.randomUUID());

    return orderBuilder.add();
  }

  public void deleteOrder(long siteId, UUID orderId) {
    client.deleteOrder(siteId, orderId);
  }

  public void printAllOrders(final long siteId) {

    for (Long contactId : contactIds) {
      List<SimpleOrder> allOrders = getSimpleOrders(siteId, contactId);
      if (allOrders == null || allOrders.isEmpty()) {
        System.out.println(String.format("No orders found for site: %d and contact: %d", siteId, contactId));
      } else {
        for (SimpleOrder simpleOrder : allOrders) {
          System.out.println(simpleOrder.toStringForMahout());
        }
      }
    }
  }

  public void printAllOrders(final long siteId, final long contactId) {

    List<SimpleOrder> allOrders = getSimpleOrders(siteId, contactId);
    if (allOrders == null || allOrders.isEmpty()) {
      System.out.println(String.format("No orders found for site: %d and contact: %d", siteId, contactId));
    } else {
      for (SimpleOrder simpleOrder : allOrders) {
        System.out.println(simpleOrder.toStringForMahout());
      }
    }
  }

  public void clearOrders(long siteId, long lastContactId) {

    for (int contactId = 1; contactId <= lastContactId; contactId++) {

      List<Order> orders = getOrders(siteId, contactId);
      for (Order order : orders) {
        client.deleteOrder(siteId, order.getOrderId());
      }
    }
  }

  public void createTestOrders(long siteId) {

    String sku = TestHelper.randomString(5);
    addOrder(siteId, 1, sku);
    addOrder(siteId, 6, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 2, sku);
    addOrder(siteId, 7, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 3, sku);
    addOrder(siteId, 8, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 4, sku);
    addOrder(siteId, 9, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 5, sku);
    addOrder(siteId, 10, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 5, sku);
    addOrder(siteId, 11, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 4, sku);
    addOrder(siteId, 12, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 3, sku);
    addOrder(siteId, 13, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 2, sku);
    addOrder(siteId, 14, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 1, sku);
    addOrder(siteId, 15, sku);

    sku = TestHelper.randomString(5);
    addOrder(siteId, 1, sku);
  }

  public static class SimpleOrder {

    private final long userId;
    private final long itemId;
    private final boolean ordered;

    private SimpleOrder(long userId, long itemId, boolean ordered) {
      this.userId = userId;
      this.itemId = itemId;
      this.ordered = ordered;
    }

    public static SimpleOrder createOrder(long userId, long itemId, boolean ordered) {
      return new SimpleOrder(userId, itemId, ordered);
    }

    public long getUserId() {
      return userId;
    }

    public long getItemId() {
      return itemId;
    }

    public boolean isOrdered() {
      return ordered;
    }

    public String toStringForMahout() {
      return userId + "," + itemId + "," + (ordered ? 1 : 0);
    }
  }

  public static void main(String[] args) throws Exception {

    SimpleOrderClient client = SimpleOrderClient.createClient();

    final long siteId = 1;

    // clear test data
    client.clearOrders(siteId, 100);
    client.printAllOrders(siteId);

    // create test data
    client.createTestOrders(siteId);
    client.printAllOrders(siteId);

    // // clear test data
    // client.clearOrders(siteId, 100);
    // client.printAllOrders(siteId);
  }
}
