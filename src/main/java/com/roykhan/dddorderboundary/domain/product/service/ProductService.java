package com.roykhan.dddorderboundary.domain.product.service;

import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductInfo findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("제품을 찾을 수 없습니다."));
        return ProductInfo.from(product);
    }

    @Transactional
    public void register(@Valid ProductInfo productInfo) {
        checkDuplicate(productInfo);

        Product product = Product.builder()
            .name(productInfo.name())
            .description(productInfo.description())
            .price(productInfo.price())
            .build();

        productRepository.save(product);
    }

    private void checkDuplicate(ProductInfo productInfo) {
        int count = productRepository.checkDuplicateProduct(productInfo);

        if(count > 0) {
            throw new RuntimeException("이미 등록된 상품입니다.");
        }
    }

    @Transactional
    public void update(Long id, ProductInfo productInfo) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("상품 정보를 찾을 수 없습니다."));

        product.setName(productInfo.name());
        product.setDescription(productInfo.description());
        product.setPrice(productInfo.price());
    }

    @Transactional
    public void delete(Long id) {
        if(!productRepository.existsById(id)) {
            throw new RuntimeException("상품 정보를 찾을 수 없습니다.");
        }

        productRepository.deleteById(id);
    }
}
