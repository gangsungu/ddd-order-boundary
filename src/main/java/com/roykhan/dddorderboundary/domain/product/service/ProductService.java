package com.roykhan.dddorderboundary.domain.product.service;

import com.roykhan.dddorderboundary.common.exception.ProductErrorCode;
import com.roykhan.dddorderboundary.domain.product.Product;
import com.roykhan.dddorderboundary.domain.product.dto.ProductInfo;
import com.roykhan.dddorderboundary.domain.product.dto.ProductRegisterRequest;
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
        Product product = productRepository.findById(id).orElseThrow(
            ProductErrorCode.PRODUCT_NOT_FOUND::exception);
        return ProductInfo.from(product);
    }

    @Transactional
    public void register(@Valid ProductRegisterRequest req) {
        checkDuplicate(req);

        Product product = Product.builder()
            .name(req.name())
            .description(req.description())
            .price(req.price())
            .build();

        productRepository.save(product);
    }

    private void checkDuplicate(ProductRegisterRequest req) {
        if(productRepository.existsByName(req.name())) {
            throw ProductErrorCode.PRODUCT_ALREADY_EXIST.exception();
        }
    }

    @Transactional
    public void update(Long id, ProductRegisterRequest req) {
        Product product = productRepository.findById(id).orElseThrow(
            ProductErrorCode.PRODUCT_NOT_FOUND::exception);

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
    }

    @Transactional
    public void delete(Long id) {
        if(!productRepository.existsById(id)) {
            throw ProductErrorCode.PRODUCT_NOT_FOUND.exception();
        }

        productRepository.deleteById(id);
    }
}
