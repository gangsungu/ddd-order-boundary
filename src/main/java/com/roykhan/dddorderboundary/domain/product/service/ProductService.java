package com.roykhan.dddorderboundary.domain.product.service;

import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductInfo findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));
        return ProductInfo.from(product);
    }
}
