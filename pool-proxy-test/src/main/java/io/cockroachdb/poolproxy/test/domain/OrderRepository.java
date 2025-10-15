package io.cockroachdb.poolproxy.test.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    @Query(value = "from Order o "
                   + "join fetch o.customer c where o.id=:id")
    Optional<Order> findOrderById(@Param("id") UUID id);

    @Query(value = "from Order o "
                   + "join fetch o.customer c")
    Page<Order> findAllOrders(Pageable page);

    @Query(value = "from Order o "
                   + "join fetch o.customer "
                   + "join fetch o.orderItems oi "
                   + "join fetch oi.product")
    Page<Order> findAllOrderDetails(Pageable page);

//    @Query(value = "from Order o "
//                   + "join fetch o.customer c "
//                   + "where c.userName=:userName")
//    List<Order> findOrdersByUserName(@Param("userName") String userName);
}
