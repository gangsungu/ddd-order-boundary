package com.roykhan.dddorderboundary.product.application.service;

import com.roykhan.dddorderboundary.product.application.dto.ProductInfo;
import com.roykhan.dddorderboundary.product.domain.exception.ProductErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Product;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.repository.ProductRepository;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import com.roykhan.dddorderboundary.product.presentation.dto.ProductRegisterRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

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

        // 재고는 상품과 같은 컨텍스트라 등록 시점에 함께 만든다
        stockRepository.save(Stock.create(product.getId(), req.initialQuantity()));
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
