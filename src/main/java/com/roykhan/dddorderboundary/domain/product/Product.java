package com.roykhan.dddorderboundary.domain.product;

import com.roykhan.dddorderboundary.domain.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Product extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(nullable = false, scale = 2, precision = 15)
    private BigDecimal price;
}
