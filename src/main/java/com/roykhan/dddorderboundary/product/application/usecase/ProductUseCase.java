package com.roykhan.dddorderboundary.product.application.usecase;

import com.roykhan.dddorderboundary.product.application.dto.ProductInfo;
import com.roykhan.dddorderboundary.product.application.dto.RegisterProductCommand;
import com.roykhan.dddorderboundary.product.application.dto.UpdateProductCommand;
import java.util.List;

// 입력 포트 - 상품 컨텍스트 바깥(컨트롤러, 다른 컨텍스트)은 이 인터페이스로만 상품을 다룬다
public interface ProductUseCase {

    ProductInfo findById(Long id);

    // 없는 ID 는 결과에서 빠진다. 빠진 게 있는지는 부르는 쪽이 판단한다
    List<ProductInfo> findAllByIds(List<Long> ids);

    Long register(RegisterProductCommand command);

    void update(Long id, UpdateProductCommand command);

    void delete(Long id);
}
