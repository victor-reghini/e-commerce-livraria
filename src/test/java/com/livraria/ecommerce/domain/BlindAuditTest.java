package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.stock.AuditStatus;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.BlindAuditItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - BlindAudit (Domínio de Conferência Cega, Bipagem de Retorno e Conciliação)")
class BlindAuditTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID auditorId = UUID.randomUUID();

    @Test
    void shouldCreateBlindAuditSuccessfully() {
        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, companyId, "Feira USP", null,
                "AUD-001", AuditStatus.ABERTA, auditorId, "Conferência pós-evento",
                new ArrayList<>(), Instant.now(), null
        );

        assertNotNull(audit.getId());
        assertEquals("AUD-001", audit.getCode());
        assertEquals(AuditStatus.ABERTA, audit.getStatus());
        assertEquals(0, audit.getTotalScanned());
        assertEquals(0, audit.getTotalExpected());
    }

    @Test
    void shouldScanNewItemAndIncrementCount() {
        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, companyId, "Feira USP", null,
                "AUD-002", AuditStatus.ABERTA, auditorId, null,
                new ArrayList<>(), Instant.now(), null
        );

        UUID skuId = UUID.randomUUID();

        // Primeira bipagem: 1 exemplar
        audit.scanBarcode(skuId, "LIV-01", "Livro Teste", 1);
        assertEquals(AuditStatus.EM_BIPAGEM, audit.getStatus());
        assertEquals(1, audit.getItems().size());
        assertEquals(1, audit.getTotalScanned());

        // Segunda bipagem: +2 exemplares
        audit.scanBarcode(skuId, "LIV-01", "Livro Teste", 2);
        assertEquals(1, audit.getItems().size());
        assertEquals(3, audit.getTotalScanned());
        assertEquals(3, audit.getItems().get(0).getQuantityScanned());
    }

    @Test
    void shouldCalculateDivergencesAccurately() {
        UUID sku1 = UUID.randomUUID();
        UUID sku2 = UUID.randomUUID();

        // Esperávamos 10 do SKU1 e 5 do SKU2
        BlindAuditItem item1 = new BlindAuditItem(UUID.randomUUID(), tenantId, UUID.randomUUID(), sku1, "SKU1", "T1", 0, 10, -10, Instant.now());
        BlindAuditItem item2 = new BlindAuditItem(UUID.randomUUID(), tenantId, UUID.randomUUID(), sku2, "SKU2", "T2", 0, 5, -5, Instant.now());

        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, companyId, "Feira", null,
                "AUD-003", AuditStatus.ABERTA, auditorId, null,
                new ArrayList<>(List.of(item1, item2)), Instant.now(), null
        );

        // Bipa 10 do SKU1 (bateu certinho: 0 divergência)
        audit.scanBarcode(sku1, "SKU1", "T1", 10);
        // Bipa 4 do SKU2 (faltou 1: divergência -1)
        audit.scanBarcode(sku2, "SKU2", "T2", 4);

        assertEquals(14, audit.getTotalScanned());
        assertEquals(15, audit.getTotalExpected());
        assertEquals(-1, audit.getTotalDivergence());
    }

    @Test
    void shouldFinalizeAndReconcileAuditLifecycle() {
        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, companyId, "Feira", null,
                "AUD-004", AuditStatus.ABERTA, auditorId, null,
                new ArrayList<>(), Instant.now(), null
        );

        audit.scanBarcode(UUID.randomUUID(), "SKU", "Titulo", 5);
        audit.finalizeAudit();

        assertEquals(AuditStatus.FINALIZADA, audit.getStatus());
        assertNotNull(audit.getCompletedAt());

        // Não permite mais bipar se finalizada
        assertThrows(BusinessException.class, () -> audit.scanBarcode(UUID.randomUUID(), "SKU2", "Titulo2", 1));

        audit.reconcile();
        assertEquals(AuditStatus.CONCILIADA, audit.getStatus());
    }

    @Test
    void shouldRejectReconcileIfNotFinalized() {
        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, companyId, "Feira", null,
                "AUD-005", AuditStatus.ABERTA, auditorId, null,
                new ArrayList<>(), Instant.now(), null
        );

        assertThrows(BusinessException.class, audit::reconcile);
    }
}

