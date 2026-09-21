package com.roykhan.dddorderboundary.product.domain.repository;

import com.roykhan.dddorderboundary.product.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByName(String productName);
}
