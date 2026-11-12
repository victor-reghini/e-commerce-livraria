package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.stock.StockTransfer;
import com.livraria.ecommerce.domain.model.stock.StockTransferItem;
import com.livraria.ecommerce.domain.model.stock.TransferStatus;
import com.livraria.ecommerce.domain.model.stock.TransferType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - StockTransfer (Domínio de Transferências, Despacho, Recebimento e Divergências)")
class StockTransferTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID originId = UUID.randomUUID();
    private final UUID destId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldCreateTransferSuccessfully() {
        UUID skuId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "LIV-01", "Livro Teste",
                10, 0, 0, new BigDecimal("20.00"), new BigDecimal("50.00")
        );

        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-001", originId, "Matriz", destId, "Feira SP",
                TransferStatus.CRIADO, TransferType.REMESSA_EVENTO, "Remessa bienal",
                List.of(item), null, null, userId, null, Instant.now(), Instant.now()
        );

        assertNotNull(transfer.getId());
        assertEquals("TRF-001", transfer.getTransferNumber());
        assertEquals(TransferStatus.CRIADO, transfer.getStatus());
        assertEquals(10, transfer.calculateTotalItems());
        assertEquals(new BigDecimal("500.00"), transfer.calculateTotalValue());
    }

    @Test
    void shouldRejectTransferWhenOriginEqualsDestination() {
        assertThrows(BusinessException.class, () -> new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-ERR", originId, "Matriz", originId, "Matriz",
                TransferStatus.CRIADO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), null, null, userId, null, Instant.now(), Instant.now()
        ));
    }

    @Test
    void shouldDispatchTransferSuccessfully() {
        StockTransferItem item1 = new StockTransferItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(), "LIV-01", "Livro 1",
                15, 0, 0, new BigDecimal("10.00"), new BigDecimal("30.00")
        );
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-002", originId, "Matriz", destId, "Feira Rio",
                TransferStatus.CRIADO, TransferType.REMESSA_EVENTO, null,
                List.of(item1), null, null, userId, null, Instant.now(), Instant.now()
        );

        transfer.dispatch(userId);

        assertEquals(TransferStatus.EM_TRANSITO, transfer.getStatus());
        assertNotNull(transfer.getDispatchedAt());
        assertEquals(15, transfer.getItems().get(0).getQuantityDispatched());
    }

    @Test
    void shouldRejectDispatchWhenNoItems() {
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-003", originId, "Matriz", destId, "Feira",
                TransferStatus.CRIADO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), null, null, userId, null, Instant.now(), Instant.now()
        );

        assertThrows(BusinessException.class, () -> transfer.dispatch(userId));
    }

    @Test
    void shouldReceiveTransferWithoutDivergence() {
        UUID skuId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "LIV-01", "Livro 1",
                20, 20, 0, new BigDecimal("10.00"), new BigDecimal("30.00")
        );
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-004", originId, "Matriz", destId, "Feira",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, null,
                List.of(item), Instant.now(), null, userId, null, Instant.now(), Instant.now()
        );

        Map<UUID, Integer> received = new HashMap<>();
        received.put(skuId, 20);

        transfer.receive(received, userId);

        assertEquals(TransferStatus.RECEBIDO, transfer.getStatus());
        assertEquals(20, item.getQuantityReceived());
        assertEquals(0, item.getDivergence());
        assertTrue(item.isFullyReceived());
        assertNotNull(transfer.getReceivedAt());
    }

    @Test
    void shouldDetectDivergenceWhenReceivedQuantityDiffers() {
        UUID skuId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "LIV-01", "Livro 1",
                20, 20, 0, new BigDecimal("10.00"), new BigDecimal("30.00")
        );
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-005", originId, "Matriz", destId, "Feira",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, null,
                List.of(item), Instant.now(), null, userId, null, Instant.now(), Instant.now()
        );

        // Despachou 20, mas só chegaram 18 (2 extraviados/faltantes)
        Map<UUID, Integer> received = new HashMap<>();
        received.put(skuId, 18);

        transfer.receive(received, userId);

        assertEquals(TransferStatus.DIVERGENCIA, transfer.getStatus());
        assertEquals(18, item.getQuantityReceived());
        assertEquals(-2, item.getDivergence());
        assertFalse(item.isFullyReceived());
    }

    @Test
    void shouldCancelTransferWhenInCreatedStatus() {
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-006", originId, "Matriz", destId, "Feira",
                TransferStatus.CRIADO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), null, null, userId, null, Instant.now(), Instant.now()
        );

        transfer.cancel();
        assertEquals(TransferStatus.CANCELADO, transfer.getStatus());
    }

    @Test
    void shouldRejectCancellationWhenInTransitOrReceived() {
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-007", originId, "Matriz", destId, "Feira",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), Instant.now(), null, userId, null, Instant.now(), Instant.now()
        );

        assertThrows(BusinessException.class, transfer::cancel);
    }
}
