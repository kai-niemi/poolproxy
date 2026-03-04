package io.cockroachdb.pool.proxy.sandbox.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

public interface OrderSystem {
    void deleteAll();

    void deleteOrders();

    void createProducts(Collection<Product> products);

    void createCustomers(Collection<Customer> customers);

    List<UUID> createOrders(Collection<Order> orders);

    Page<Product> listAllProducts(int limit);

    Page<Customer> listAllCustomers(int limit);

    Page<Order> listAllOrders(int limit);

    Page<Order> listAllOrdersWithDetails(int limit);

    BigDecimal getTotalOrderPrice();
}
