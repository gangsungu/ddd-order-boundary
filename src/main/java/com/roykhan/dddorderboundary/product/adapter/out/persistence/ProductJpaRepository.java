package com.roykhan.dddorderboundary.product.adapter.out.persistence;

import com.roykhan.dddorderboundary.product.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    boolean existsByName(String name);
}
