package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.AuthorRole;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.IsbnValidator;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.in.BatchImportUseCase;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.domain.port.out.ProductRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class BatchImportService implements BatchImportUseCase {

    private static final Logger log = LoggerFactory.getLogger(BatchImportService.class);
    private static final int CHUNK_SIZE = 500;

    private final ProductRepositoryPort productRepository;
    private final SkuRepositoryPort skuRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final AuthorRepositoryPort authorRepository;
    private final PublisherRepositoryPort publisherRepository;
    private final StockRepositoryPort stockRepository;
    private final PlatformTransactionManager transactionManager;

    public BatchImportService(ProductRepositoryPort productRepository,
                              SkuRepositoryPort skuRepository,
                              CategoryRepositoryPort categoryRepository,
                              AuthorRepositoryPort authorRepository,
                              PublisherRepositoryPort publisherRepository,
                              StockRepositoryPort stockRepository,
                              PlatformTransactionManager transactionManager) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.stockRepository = stockRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public CatalogDto.BatchImportResult importSpreadsheet(UUID tenantId, UUID companyId, InputStream inputStream, String fileName) {
        long startTime = System.currentTimeMillis();
        List<Map<String, String>> rows = parseSpreadsheet(inputStream, fileName);

        int totalProcessed = rows.size();
        int successCount = 0;
        int failureCount = 0;
        List<CatalogDto.BatchImportRowError> errors = new ArrayList<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // Process in chunks of 500
        for (int i = 0; i < rows.size(); i += CHUNK_SIZE) {
            int toIndex = Math.min(i + CHUNK_SIZE, rows.size());
            List<Map<String, String>> chunk = rows.subList(i, toIndex);

            for (int j = 0; j < chunk.size(); j++) {
                int rowNumber = i + j + 2; // +1 for 0-index, +1 for header row
                Map<String, String> rowData = chunk.get(j);
                String skuCode = rowData.getOrDefault("sku", "N/A");
                String title = rowData.getOrDefault("titulo", "N/A");

                try {
                    transactionTemplate.execute(status -> {
                        importSingleRow(tenantId, companyId, rowData);
                        return null;
                    });
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                    String msg = ex.getMessage();
                    if (ex.getCause() != null) {
                        msg = ex.getCause().getMessage();
                    }
                    errors.add(new CatalogDto.BatchImportRowError(rowNumber, skuCode, title, msg));
                    log.warn("Falha ao importar linha {}: sku={}, erro={}", rowNumber, skuCode, msg);
                }
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        log.info("Importação em lote concluída: total={}, sucesso={}, falhas={}, tempoMs={}",
                totalProcessed, successCount, failureCount, executionTime);

        return new CatalogDto.BatchImportResult(totalProcessed, successCount, failureCount, errors, executionTime);
    }

    private void importSingleRow(UUID tenantId, UUID companyId, Map<String, String> data) {
        String title = getRequiredField(data, "titulo", "title", "nome");
        String skuCode = getRequiredField(data, "sku", "codigo", "isbn");

        String subtitle = getOptionalField(data, "subtitulo", "subtitle");
        String authorName = getOptionalField(data, "autor", "autores", "author");
        String publisherName = getOptionalField(data, "editora", "publisher");
        String categoryName = getOptionalField(data, "categoria", "category", "assunto");
        String priceStr = getOptionalField(data, "preco", "price", "valor");
        String costPriceStr = getOptionalField(data, "custo", "cost", "preco_custo");
        String yearStr = getOptionalField(data, "ano", "ano_edicao", "year");
        String pagesStr = getOptionalField(data, "paginas", "pages");
        String weightStr = getOptionalField(data, "peso", "peso_gramas", "weight");
        String bindingStr = getOptionalField(data, "encadernacao", "binding");
        String stockStr = getOptionalField(data, "estoque", "estoque_inicial", "quantidade", "stock");
        String ncm = getOptionalField(data, "ncm");
        String cfop = getOptionalField(data, "cfop");
        String barcode = getOptionalField(data, "barcode", "ean", "ean13", "codigo_barras");
        String isbn = getOptionalField(data, "isbn");

        // Parse price
        BigDecimal price = BigDecimal.ZERO;
        if (priceStr != null && !priceStr.isBlank()) {
            price = parseBigDecimal(priceStr);
        }

        BigDecimal costPrice = BigDecimal.ZERO;
        if (costPriceStr != null && !costPriceStr.isBlank()) {
            costPrice = parseBigDecimal(costPriceStr);
        }

        // Category resolution
        UUID categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            String catSlug = Product.generateSlug(categoryName);
            Category category = categoryRepository.findBySlug(tenantId, catSlug)
                    .orElseGet(() -> categoryRepository.save(Category.create(tenantId, categoryName.trim(), catSlug, null)));
            categoryId = category.getId();
        }

        // Publisher resolution
        UUID publisherId = null;
        if (publisherName != null && !publisherName.isBlank()) {
            Publisher publisher = publisherRepository.findByName(tenantId, publisherName.trim())
                    .orElseGet(() -> publisherRepository.save(Publisher.create(tenantId, publisherName.trim())));
            publisherId = publisher.getId();
        }

        final UUID resolvedCategoryId = categoryId;
        final UUID resolvedPublisherId = publisherId;

        // Product resolution
        String slug = Product.generateSlug(title);
        Product product = productRepository.findBySlug(tenantId, slug)
                .orElseGet(() -> Product.create(tenantId, resolvedCategoryId, resolvedPublisherId, title, subtitle, slug, null, ProductType.BOOK));

        if (product.getId() != null && (categoryId != null || publisherId != null)) {
            product.update(product.getTitle(), product.getSubtitle(), product.getSlug(),
                    product.getSynopsis(),
                    categoryId != null ? categoryId : product.getCategoryId(),
                    publisherId != null ? publisherId : product.getPublisherId(),
                    product.getProductType(), product.isActive());
        }

        // Author resolution
        if (authorName != null && !authorName.isBlank()) {
            Author author = authorRepository.findByName(tenantId, authorName.trim())
                    .orElseGet(() -> authorRepository.save(Author.create(tenantId, authorName.trim(), null)));
            product.addAuthor(author.getId(), author.getName(), AuthorRole.AUTHOR, 0);
        }
        product = productRepository.save(product);

        // Binding type
        BindingType bindingType = BindingType.BROCHURA;
        if (bindingStr != null && !bindingStr.isBlank()) {
            String normalized = bindingStr.toUpperCase();
            if (normalized.contains("DURA")) bindingType = BindingType.CAPA_DURA;
            else if (normalized.contains("ESPIRAL")) bindingType = BindingType.ESPIRAL;
            else if (normalized.contains("EBOOK") || normalized.contains("DIGITAL")) bindingType = BindingType.DIGITAL_EBOOK;
        }

        Integer editionYear = (yearStr != null && !yearStr.isBlank()) ? parseIntSafe(yearStr) : null;
        Integer pages = (pagesStr != null && !pagesStr.isBlank()) ? parseIntSafe(pagesStr) : null;
        int weightGrams = (weightStr != null && !weightStr.isBlank()) ? parseIntSafe(weightStr) : 0;

        // ISBN validation
        String validIsbn = null;
        if (isbn != null && !isbn.isBlank()) {
            String cleaned = IsbnValidator.clean(isbn);
            if (IsbnValidator.isValid(cleaned)) {
                validIsbn = cleaned;
            }
        }

        // Check SKU
        Optional<Sku> existingSku = skuRepository.findBySkuCode(tenantId, skuCode.trim());
        Sku sku;
        if (existingSku.isPresent()) {
            sku = existingSku.get();
            sku.updateDetails(
                    barcode != null ? barcode : sku.getBarcode(),
                    validIsbn != null ? validIsbn : sku.getIsbn(),
                    bindingType,
                    editionYear != null ? editionYear : sku.getEditionYear(),
                    sku.getVolume(),
                    pages != null ? pages : sku.getPages(),
                    sku.getCatalogingSheetText(),
                    weightGrams > 0 ? weightGrams : sku.getWeightGrams(),
                    sku.getDimensions(),
                    price.compareTo(BigDecimal.ZERO) > 0 ? price : sku.getPrice(),
                    sku.getPromotionalPrice(),
                    costPrice.compareTo(BigDecimal.ZERO) > 0 ? costPrice : sku.getCostPrice(),
                    ncm != null ? ncm : sku.getNcm(),
                    cfop != null ? cfop : sku.getCfop(),
                    true
            );
            sku = skuRepository.save(sku);
        } else {
            sku = new Sku(
                    UUID.randomUUID(),
                    tenantId,
                    product.getId(),
                    skuCode.trim(),
                    barcode != null ? barcode : skuCode.trim(),
                    validIsbn,
                    bindingType,
                    editionYear,
                    null,
                    pages,
                    null,
                    weightGrams,
                    Dimensions.zero(),
                    price,
                    null,
                    costPrice,
                    ncm != null ? ncm : "4901.99.00",
                    cfop != null ? cfop : "5102",
                    true,
                    Collections.emptyList(),
                    null,
                    Collections.emptyList(),
                    Instant.now(),
                    Instant.now()
            );
            sku = skuRepository.save(sku);
        }

        // Stock quantity
        if (companyId != null && stockStr != null && !stockStr.isBlank()) {
            int qty = parseIntSafe(stockStr);
            stockRepository.setStockQuantity(tenantId, companyId, sku.getId(), qty);
        }
    }

    private List<Map<String, String>> parseSpreadsheet(InputStream inputStream, String fileName) {
        String lowerName = fileName != null ? fileName.toLowerCase() : "";
        if (lowerName.endsWith(".csv")) {
            return parseCsv(inputStream);
        } else {
            return parseExcel(inputStream);
        }
    }

    private List<Map<String, String>> parseCsv(InputStream inputStream) {
        List<Map<String, String>> result = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : parser) {
                Map<String, String> row = new HashMap<>();
                for (String header : record.getParser().getHeaderNames()) {
                    row.put(normalizeKey(header), record.get(header));
                }
                result.add(row);
            }
        } catch (Exception e) {
            throw new BusinessException("Erro ao processar arquivo CSV: " + e.getMessage(), e);
        }
        return result;
    }

    private List<Map<String, String>> parseExcel(InputStream inputStream) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            if (!rowIterator.hasNext()) {
                return result;
            }

            // Header row
            Row headerRow = rowIterator.next();
            Map<Integer, String> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell);
                if (header != null && !header.isBlank()) {
                    headerMap.put(cell.getColumnIndex(), normalizeKey(header));
                }
            }

            // Data rows
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowData = new HashMap<>();
                boolean hasContent = false;

                for (Map.Entry<Integer, String> entry : headerMap.entrySet()) {
                    Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = getCellValueAsString(cell);
                    if (val != null && !val.isBlank()) {
                        hasContent = true;
                        rowData.put(entry.getValue(), val.trim());
                    }
                }

                if (hasContent) {
                    result.add(rowData);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Erro ao processar planilha Excel: " + e.getMessage(), e);
        }
        return result;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            }
            double val = cell.getNumericCellValue();
            if (val == (long) val) {
                return String.valueOf((long) val);
            }
            return String.valueOf(val);
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return Product.generateSlug(key).replace("-", "_").toLowerCase();
    }

    private String getRequiredField(Map<String, String> data, String... aliases) {
        for (String alias : aliases) {
            String val = data.get(normalizeKey(alias));
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        throw new IllegalArgumentException("Coluna obrigatória não encontrada. Aliases esperados: " + String.join(", ", aliases));
    }

    private String getOptionalField(Map<String, String> data, String... aliases) {
        for (String alias : aliases) {
            String val = data.get(normalizeKey(alias));
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }

    private BigDecimal parseBigDecimal(String val) {
        try {
            String cleaned = val.replaceAll("[^0-9,.]", "").replace(",", ".");
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private int parseIntSafe(String val) {
        try {
            String cleaned = val.replaceAll("[^0-9]", "");
            return Integer.parseInt(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }
}
