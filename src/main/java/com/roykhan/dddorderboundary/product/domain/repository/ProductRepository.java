package com.roykhan.dddorderboundary.product.domain.repository;

import com.roykhan.dddorderboundary.product.domain.model.Product;
import java.util.List;
import java.util.Optional;

// 출력 포트 - 상품 저장소. 구현은 infrastructure/persistence 의 어댑터가 맡는다
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(Long id);

    List<Product> findAllById(List<Long> ids);

    boolean existsById(Long id);

    boolean existsByName(String name);

    void deleteById(Long id);
}
