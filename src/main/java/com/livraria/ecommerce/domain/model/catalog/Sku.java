package com.livraria.ecommerce.domain.model.catalog;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Sku {

    private UUID id;
    private UUID tenantId;
    private UUID productId;
    private String skuCode;
    private String barcode;
    private String isbn;
    private BindingType bindingType;
    private Integer editionYear;
    private String volume;
    private Integer pages;
    private String catalogingSheetText;
    private int weightGrams;
    private Dimensions dimensions;
    private BigDecimal price;
    private BigDecimal promotionalPrice;
    private BigDecimal costPrice;
    private String ncm;
    private String cfop;
    private boolean active;
    private List<BundleItem> bundleItems;
    private EbookMetadata ebookMetadata;
    private List<ProductImage> images;
    private Instant createdAt;
    private Instant updatedAt;

    public Sku(UUID id, UUID tenantId, UUID productId, String skuCode, String barcode, String isbn,
               BindingType bindingType, Integer editionYear, String volume, Integer pages,
               String catalogingSheetText, int weightGrams, Dimensions dimensions,
               BigDecimal price, BigDecimal promotionalPrice, BigDecimal costPrice,
               String ncm, String cfop, boolean active, List<BundleItem> bundleItems,
               EbookMetadata ebookMetadata, List<ProductImage> images,
               Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.productId = productId;
        this.skuCode = skuCode;
        this.barcode = barcode;
        this.isbn = IsbnValidator.clean(isbn);
        this.bindingType = bindingType != null ? bindingType : BindingType.BROCHURA;
        this.editionYear = editionYear;
        this.volume = volume;
        this.pages = pages;
        this.catalogingSheetText = catalogingSheetText;
        this.weightGrams = Math.max(0, weightGrams);
        this.dimensions = dimensions != null ? dimensions : Dimensions.zero();
        this.price = price != null ? price : BigDecimal.ZERO;
        this.promotionalPrice = promotionalPrice;
        this.costPrice = costPrice != null ? costPrice : BigDecimal.ZERO;
        this.ncm = ncm != null ? ncm : "4901.99.00";
        this.cfop = cfop != null ? cfop : "5102";
        this.active = active;
        this.bundleItems = bundleItems != null ? new ArrayList<>(bundleItems) : new ArrayList<>();
        this.ebookMetadata = ebookMetadata;
        this.images = images != null ? new ArrayList<>(images) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        validate();
    }

    private void validate() {
        if (skuCode == null || skuCode.isBlank()) {
            throw new IllegalArgumentException("O código SKU é obrigatório.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("O preço do SKU não pode ser negativo.");
        }
        if (isbn != null && !isbn.isBlank() && !IsbnValidator.isValid(isbn)) {
            throw new BusinessException("O ISBN informado (" + isbn + ") é inválido segundo a norma internacional.");
        }
    }

    public boolean isBundle() {
        return !bundleItems.isEmpty();
    }

    public boolean isEbook() {
        return ebookMetadata != null || bindingType == BindingType.DIGITAL_EBOOK;
    }

    public BigDecimal getEffectivePrice() {
        if (promotionalPrice != null && promotionalPrice.compareTo(BigDecimal.ZERO) > 0
                && promotionalPrice.compareTo(price) < 0) {
            return promotionalPrice;
        }
        return price;
    }

    /**
     * Calcula o saldo virtual disponível para este SKU.
     * Para kits dinâmicos, a disponibilidade é o mínimo entre os saldos dos itens constituintes
     * dividido pela quantidade necessária de cada item no kit.
     */
    public int calculateVirtualStock(Map<UUID, Integer> stockBySkuId) {
        if (!isBundle()) {
            return stockBySkuId != null ? stockBySkuId.getOrDefault(this.id, 0) : 0;
        }

        // Se algum item é físico pré-montado, pode ter saldo direto no estoque
        boolean hasDynamicVirtual = bundleItems.stream()
                .anyMatch(item -> item.getBundleType() == BundleType.DYNAMIC_VIRTUAL);

        if (!hasDynamicVirtual) {
            return stockBySkuId != null ? stockBySkuId.getOrDefault(this.id, 0) : 0;
        }

        if (stockBySkuId == null || stockBySkuId.isEmpty()) {
            return 0;
        }

        int maxKits = Integer.MAX_VALUE;
        for (BundleItem item : bundleItems) {
            int availableUnits = stockBySkuId.getOrDefault(item.getItemSkuId(), 0);
            int possible = availableUnits / item.getQuantity();
            if (possible < maxKits) {
                maxKits = possible;
            }
        }
        return Math.max(0, maxKits == Integer.MAX_VALUE ? 0 : maxKits);
    }

    /**
     * Decompõe o valor faturado do Kit para emissão de NF-e / NFC-e,
     * aplicando o rateio proporcional com NCM e CFOP específicos de cada componente.
     */
    public List<FiscalDecompositionItem> decomposeBundleForFiscal(BigDecimal invoicedTotal) {
        if (!isBundle()) {
            return List.of(new FiscalDecompositionItem(
                    this.id,
                    this.skuCode,
                    "Item Padrão",
                    1,
                    this.price,
                    invoicedTotal,
                    invoicedTotal,
                    this.ncm,
                    this.cfop
            ));
        }

        if (invoicedTotal == null || invoicedTotal.compareTo(BigDecimal.ZERO) <= 0) {
            invoicedTotal = getEffectivePrice();
        }

        BigDecimal originalItemsTotal = BigDecimal.ZERO;
        for (BundleItem item : bundleItems) {
            BigDecimal lineTotal = item.getItemPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            originalItemsTotal = originalItemsTotal.add(lineTotal);
        }

        List<FiscalDecompositionItem> results = new ArrayList<>();
        BigDecimal accumulatedProportionalTotal = BigDecimal.ZERO;

        for (int i = 0; i < bundleItems.size(); i++) {
            BundleItem item = bundleItems.get(i);
            boolean isLast = (i == bundleItems.size() - 1);

            BigDecimal lineProportionalTotal;
            if (isLast) {
                // Ajuste de arredondamento no último item para soma exata
                lineProportionalTotal = invoicedTotal.subtract(accumulatedProportionalTotal);
            } else {
                if (originalItemsTotal.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lineOriginal = item.getItemPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    lineProportionalTotal = invoicedTotal.multiply(lineOriginal)
                            .divide(originalItemsTotal, 2, RoundingMode.HALF_UP);
                } else {
                    lineProportionalTotal = invoicedTotal.divide(BigDecimal.valueOf(bundleItems.size()), 2, RoundingMode.HALF_UP);
                }
                accumulatedProportionalTotal = accumulatedProportionalTotal.add(lineProportionalTotal);
            }

            BigDecimal unitPrice = lineProportionalTotal.divide(BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);

            results.add(new FiscalDecompositionItem(
                    item.getItemSkuId(),
                    item.getItemSkuCode(),
                    item.getItemTitle(),
                    item.getQuantity(),
                    item.getItemPrice(),
                    unitPrice,
                    lineProportionalTotal,
                    item.getItemNcm(),
                    item.getItemCfop()
            ));
        }

        return results;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public String getSkuCode() { return skuCode; }
    public String getBarcode() { return barcode; }
    public String getIsbn() { return isbn; }
    public BindingType getBindingType() { return bindingType; }
    public Integer getEditionYear() { return editionYear; }
    public String getVolume() { return volume; }
    public Integer getPages() { return pages; }
    public String getCatalogingSheetText() { return catalogingSheetText; }
    public int getWeightGrams() { return weightGrams; }
    public Dimensions getDimensions() { return dimensions; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getPromotionalPrice() { return promotionalPrice; }
    public BigDecimal getCostPrice() { return costPrice; }
    public String getNcm() { return ncm; }
    public String getCfop() { return cfop; }
    public boolean isActive() { return active; }
    public List<BundleItem> getBundleItems() { return Collections.unmodifiableList(bundleItems); }
    public EbookMetadata getEbookMetadata() { return ebookMetadata; }
    public List<ProductImage> getImages() { return Collections.unmodifiableList(images); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateDetails(String barcode, String isbn, BindingType bindingType, Integer editionYear,
                              String volume, Integer pages, String catalogingSheetText, int weightGrams,
                              Dimensions dimensions, BigDecimal price, BigDecimal promotionalPrice,
                              BigDecimal costPrice, String ncm, String cfop, boolean active) {
        this.barcode = barcode;
        this.isbn = IsbnValidator.clean(isbn);
        this.bindingType = bindingType != null ? bindingType : this.bindingType;
        this.editionYear = editionYear;
        this.volume = volume;
        this.pages = pages;
        this.catalogingSheetText = catalogingSheetText;
        this.weightGrams = Math.max(0, weightGrams);
        this.dimensions = dimensions != null ? dimensions : this.dimensions;
        this.price = price != null ? price : this.price;
        this.promotionalPrice = promotionalPrice;
        this.costPrice = costPrice != null ? costPrice : this.costPrice;
        this.ncm = ncm != null ? ncm : this.ncm;
        this.cfop = cfop != null ? cfop : this.cfop;
        this.active = active;
        this.updatedAt = Instant.now();
        validate();
    }

    public void setBundleItems(List<BundleItem> items) {
        this.bundleItems = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public void setEbookMetadata(EbookMetadata metadata) {
        this.ebookMetadata = metadata;
        this.updatedAt = Instant.now();
    }
}

