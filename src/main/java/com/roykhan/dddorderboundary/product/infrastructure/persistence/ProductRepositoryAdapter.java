package com.roykhan.dddorderboundary.product.infrastructure.persistence;

import com.roykhan.dddorderboundary.product.domain.model.Product;
import com.roykhan.dddorderboundary.product.domain.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

// 출력 어댑터 - 상품 저장소 포트를 Spring Data JPA 로 구현한다
@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public List<Product> findAllById(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public boolean existsById(Long id) {
        return productJpaRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return productJpaRepository.existsByName(name);
    }

    @Override
    public void deleteById(Long id) {
        productJpaRepository.deleteById(id);
    }
}
