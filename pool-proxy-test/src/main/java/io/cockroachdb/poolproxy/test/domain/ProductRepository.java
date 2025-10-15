package io.cockroachdb.poolproxy.test.domain;

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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findProductBySku(String sku);

    @Query("select p from Product p where p.sku=:sku")
    Optional<Product> findProductBySkuNoLock(@Param("sku") String sku);
}
