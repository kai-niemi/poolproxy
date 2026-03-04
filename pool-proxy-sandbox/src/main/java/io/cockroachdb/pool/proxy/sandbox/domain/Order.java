package io.cockroachdb.pool.proxy.sandbox.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order extends AbstractEntity<UUID> {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Customer customer;

        private ShipmentStatus shipmentStatus = ShipmentStatus.placed;

        private final List<OrderItem> orderItems = new ArrayList<>();

        private Builder() {
        }

        public Builder withCustomer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public Builder withShipmentStatus(ShipmentStatus shipmentStatus) {
            this.shipmentStatus = shipmentStatus;
            return this;
        }

        public OrderItem.Builder andOrderItem() {
            return new OrderItem.Builder(this, orderItems::add);
        }

        public Order build() {
            if (this.customer == null) {
                throw new IllegalStateException("Missing customer");
            }
            if (this.orderItems.isEmpty()) {
                throw new IllegalStateException("Empty order");
            }
            Order order = new Order();
            order.shipmentStatus = this.shipmentStatus;
            order.customer = this.customer;
            order.deliveryAddress = customer.getAddress();
            order.orderItems.addAll(this.orderItems);
            order.totalPrice = order.subTotal();
            return order;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "total_price", nullable = false, updatable = false)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @ElementCollection(fetch = FetchType.LAZY)
    @JoinTable(name = "order_items",
            joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "item_pos")
    @Fetch(FetchMode.SUBSELECT)
    private List<OrderItem> orderItems = new ArrayList<>();

//    @ElementCollection(fetch = FetchType.LAZY)
//    @JoinColumn(name = "order_id", nullable = false)
//    @JoinTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
//    @OrderColumn(name = "item_pos")
//    private List<OrderItem> orderItems = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 25, nullable = false)
    private ShipmentStatus shipmentStatus;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, updatable = false, name = "date_placed")
    private LocalDateTime datePlaced;

    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, name = "date_updated")
    private LocalDateTime dateUpdated;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address1",
                    column = @Column(name = "deliv_address1")),
            @AttributeOverride(name = "address2",
                    column = @Column(name = "deliv_address2")),
            @AttributeOverride(name = "city",
                    column = @Column(name = "deliv_city")),
            @AttributeOverride(name = "postcode",
                    column = @Column(name = "deliv_postcode")),
            @AttributeOverride(name = "country",
                    column = @Column(name = "deliv_country"))
    })
    private Address deliveryAddress;

    @Override
    public UUID getId() {
        return id;
    }

    @PrePersist
    protected void onCreate() {
        if (datePlaced == null) {
            datePlaced = LocalDateTime.now();
        }
        if (dateUpdated == null) {
            dateUpdated = LocalDateTime.now();
        }
    }

    public Order setId(UUID id) {
        this.id = id;
        return this;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<OrderItem> getOrderItems() {
        return Collections.unmodifiableList(orderItems);
    }

    public BigDecimal subTotal() {
        BigDecimal subTotal = BigDecimal.ZERO;
        for (OrderItem oi : orderItems) {
            subTotal = subTotal.add(oi.totalCost());
        }
        return subTotal;
    }
}
