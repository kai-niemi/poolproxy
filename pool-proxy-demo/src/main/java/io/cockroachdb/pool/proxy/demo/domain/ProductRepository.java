package io.cockroachdb.pool.proxy.demo.domain;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("select p from Product p where p.sku=:sku")
    Optional<Product> findProductBySku(@Param("sku") String sku);

    @Query("select p from Product p where p.sku=:sku")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findProductBySkuForUpdate(@Param("sku") String sku);
}
