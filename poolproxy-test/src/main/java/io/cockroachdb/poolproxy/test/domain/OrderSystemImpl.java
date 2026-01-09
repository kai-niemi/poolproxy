package io.cockroachdb.poolproxy.test.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

@Service
public class OrderSystemImpl implements OrderSystem {
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAll() {
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "TX not active");

        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteOrders() {
        orderRepository.deleteAllInBatch();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProducts(Collection<Product> products) {
        Assert.isTrue(!TransactionSynchronizationManager.isCurrentTransactionReadOnly(), "Not read-only");
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "No tx");

        productRepository.saveAll(products);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createCustomers(Collection<Customer> customers) {
        Assert.isTrue(!TransactionSynchronizationManager.isCurrentTransactionReadOnly(), "Not read-only");
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "No tx");

        customerRepository.saveAll(customers);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> createOrders(Collection<Order> orders) {
        Assert.isTrue(!TransactionSynchronizationManager.isCurrentTransactionReadOnly(), "Not read-only");
        Assert.isTrue(TransactionSynchronizationManager.isActualTransactionActive(), "No tx");

        orderRepository.saveAll(orders);

        return orders.stream().map(Order::getId).toList();
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Page<Customer> listAllCustomers(int limit) {
        return customerRepository.findAll(PageRequest.ofSize(limit));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Page<Product> listAllProducts(int limit) {
        return productRepository.findAll(PageRequest.ofSize(limit));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Page<Order> listAllOrders(int limit) {
        return orderRepository.findAllOrders(PageRequest.ofSize(limit));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public Page<Order> listAllOrdersWithDetails(int limit) {
        return orderRepository.findAllOrderDetails(PageRequest.ofSize(limit));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public BigDecimal getTotalOrderPrice() {
        BigDecimal price = BigDecimal.ZERO;
        List<Order> orders = orderRepository.findAll();
        for (Order order : orders) {
            price = price.add(order.getTotalPrice());
        }
        return price;
    }
}
