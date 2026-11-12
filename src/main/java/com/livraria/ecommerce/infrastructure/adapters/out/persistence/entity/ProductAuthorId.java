package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProductAuthorId implements Serializable {
    private UUID productId;
    private UUID authorId;
    private String role;
}

