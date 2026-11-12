package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private final UUID tenantId = UUID.randomUUID();
    private final String sessionId = "sess_123456";

    @Test
    @DisplayName("Deve criar carrinho válido e calcular subtotal e total de itens")
    void shouldCreateCartAndCalculateTotals() {
        Cart cart = Cart.createNew(tenantId, sessionId, null);
        assertThat(cart.isEmpty()).isTrue();

        UUID sku1 = UUID.randomUUID();
        UUID sku2 = UUID.randomUUID();

        cart.addItem(sku1, "SKU-001", "Livro Clean Architecture", 2, new BigDecimal("80.00"));
        cart.addItem(sku2, "SKU-002", "Livro Domain-Driven Design", 1, new BigDecimal("120.00"));

        assertThat(cart.isEmpty()).isFalse();
        assertThat(cart.calculateTotalItems()).isEqualTo(3);
        assertThat(cart.calculateSubtotal()).isEqualByComparingTo(new BigDecimal("280.00"));
    }

    @Test
    @DisplayName("Deve acumular quantidade ao adicionar o mesmo SKU repetidamente")
    void shouldAccumulateQuantityForSameSku() {
        Cart cart = Cart.createNew(tenantId, sessionId, null);
        UUID sku1 = UUID.randomUUID();

        cart.addItem(sku1, "SKU-001", "Livro Clean Architecture", 1, new BigDecimal("80.00"));
        cart.addItem(sku1, "SKU-001", "Livro Clean Architecture", 2, new BigDecimal("80.00"));

        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(cart.calculateSubtotal()).isEqualByComparingTo(new BigDecimal("240.00"));
    }

    @Test
    @DisplayName("Deve atualizar quantidade de um item ou remover se quantidade for zero")
    void shouldUpdateQuantityOrRemoveWhenZero() {
        Cart cart = Cart.createNew(tenantId, sessionId, null);
        UUID sku1 = UUID.randomUUID();

        cart.addItem(sku1, "SKU-001", "Livro", 5, new BigDecimal("50.00"));
        cart.updateItemQuantity(sku1, 3);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(3);

        cart.updateItemQuantity(sku1, 0);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Deve remover item e esvaziar carrinho corretamente")
    void shouldRemoveItemAndClear() {
        Cart cart = Cart.createNew(tenantId, sessionId, null);
        UUID sku1 = UUID.randomUUID();
        UUID sku2 = UUID.randomUUID();

        cart.addItem(sku1, "SKU-001", "Livro 1", 1, new BigDecimal("50.00"));
        cart.addItem(sku2, "SKU-002", "Livro 2", 1, new BigDecimal("70.00"));

        cart.removeItem(sku1);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getSkuId()).isEqualTo(sku2);

        cart.clear();
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Deve rejeitar criação de carrinho sem tenantId ou sem identificador")
    void shouldRejectInvalidCartCreation() {
        assertThatThrownBy(() -> new Cart(null, null, sessionId, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tenant ID é obrigatório");

        assertThatThrownBy(() -> new Cart(null, tenantId, null, null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vinculado a uma sessão ou a um cliente");
    }
}
