package com.roykhan.dddorderboundary.domain.product.repository;

import com.roykhan.dddorderboundary.domain.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
