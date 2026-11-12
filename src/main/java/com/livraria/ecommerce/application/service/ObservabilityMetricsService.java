package com.livraria.ecommerce.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityMetricsService {

    private final MeterRegistry meterRegistry;

    private final Counter orderCreatedCounter;
    private final Counter orderPaidCounter;
    private final Counter posSaleCompletedCounter;
    private final Counter invoiceCanceledCounter;
    private final Timer sefazTransmissionTimer;

    public ObservabilityMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.orderCreatedCounter = Counter.builder("ecommerce.orders.created")
                .description("Total de pedidos criados no e-commerce")
                .register(meterRegistry);

        this.orderPaidCounter = Counter.builder("ecommerce.orders.paid")
                .description("Total de pedidos pagos com sucesso")
                .register(meterRegistry);

        this.posSaleCompletedCounter = Counter.builder("pos.sales.completed")
                .description("Total de vendas concluídas no PDV físico")
                .register(meterRegistry);

        this.invoiceCanceledCounter = Counter.builder("fiscal.invoices.canceled")
                .description("Total de notas fiscais canceladas")
                .register(meterRegistry);

        this.sefazTransmissionTimer = Timer.builder("sefaz.transmission.time")
                .description("Latência de comunicação com a SEFAZ")
                .register(meterRegistry);
    }

    public void incrementOrdersCreated() {
        orderCreatedCounter.increment();
    }

    public void incrementOrdersPaid() {
        orderPaidCounter.increment();
    }

    public void incrementPosSalesCompleted() {
        posSaleCompletedCounter.increment();
    }

    public void recordInvoiceEmitted(String model, String status) {
        Counter.builder("fiscal.invoices.emitted")
                .description("Total de notas fiscais emitidas por modelo e status")
                .tag("model", model != null ? model : "55")
                .tag("status", status != null ? status : "AUTORIZADA")
                .register(meterRegistry)
                .increment();
    }

    public void incrementInvoicesCanceled() {
        invoiceCanceledCounter.increment();
    }

    public void recordAuditEvent(String action) {
        Counter.builder("audit.events.recorded")
                .description("Total de eventos de auditoria registrados")
                .tag("action", action != null ? action : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public void recordSefazLatency(long durationMs) {
        sefazTransmissionTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}

