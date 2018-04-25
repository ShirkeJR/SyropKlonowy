package com.blinenterprise.SyropKlonowy.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Getter
@ToString
@NoArgsConstructor
@Entity
public class OrderedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @OneToOne
    private Product product;

    private Integer quantity;

    public OrderedProduct(Product product, Integer quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}
