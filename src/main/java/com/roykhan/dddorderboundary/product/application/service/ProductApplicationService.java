package com.roykhan.dddorderboundary.product.application.service;

import com.roykhan.dddorderboundary.product.application.dto.ProductInfo;
import com.roykhan.dddorderboundary.product.application.dto.RegisterProductCommand;
import com.roykhan.dddorderboundary.product.application.dto.UpdateProductCommand;
import com.roykhan.dddorderboundary.product.application.usecase.ProductUseCase;
import com.roykhan.dddorderboundary.product.domain.exception.ProductErrorCode;
import com.roykhan.dddorderboundary.product.domain.model.Product;
import com.roykhan.dddorderboundary.product.domain.model.Stock;
import com.roykhan.dddorderboundary.product.domain.repository.ProductRepository;
import com.roykhan.dddorderboundary.product.domain.repository.StockRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductApplicationService implements ProductUseCase {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductInfo findById(Long id) {
        return ProductInfo.from(getProduct(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductInfo> findAllByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
            .map(ProductInfo::from)
            .toList();
    }

    @Override
    @Transactional
    public Long register(RegisterProductCommand command) {
        checkDuplicate(command.name());

        Product product = Product.builder()
            .name(command.name())
            .description(command.description())
            .price(command.price())
            .build();

        productRepository.save(product);

        // 재고는 상품과 같은 컨텍스트라 등록 시점에 함께 만든다
        stockRepository.save(Stock.create(product.getId(), command.initialQuantity()));

        return product.getId();
    }

    @Override
    @Transactional
    public void update(Long id, UpdateProductCommand command) {
        getProduct(id).update(command.name(), command.description(), command.price());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if(!productRepository.existsById(id)) {
            throw ProductErrorCode.PRODUCT_NOT_FOUND.exception();
        }

        productRepository.deleteById(id);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(ProductErrorCode.PRODUCT_NOT_FOUND::exception);
    }

    private void checkDuplicate(String name) {
        if(productRepository.existsByName(name)) {
            throw ProductErrorCode.PRODUCT_ALREADY_EXIST.exception();
        }
    }
}
