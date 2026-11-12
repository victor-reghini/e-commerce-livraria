package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.application.service.BatchImportService;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.domain.port.out.ProductRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - BatchImportService (Streaming & Isolamento de Rollback)")
class BatchImportServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;
    @Mock
    private SkuRepositoryPort skuRepository;
    @Mock
    private CategoryRepositoryPort categoryRepository;
    @Mock
    private AuthorRepositoryPort authorRepository;
    @Mock
    private PublisherRepositoryPort publisherRepository;
    @Mock
    private StockRepositoryPort stockRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private BatchImportService batchImportService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(new SimpleTransactionStatus());

        batchImportService = new BatchImportService(
                productRepository, skuRepository, categoryRepository,
                authorRepository, publisherRepository, stockRepository, transactionManager
        );

        when(categoryRepository.findBySlug(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        when(publisherRepository.findByName(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(publisherRepository.save(any(Publisher.class))).thenAnswer(i -> i.getArgument(0));

        when(authorRepository.findByName(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenAnswer(i -> i.getArgument(0));

        when(productRepository.findBySlug(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        when(skuRepository.findBySkuCode(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldImportCsvSuccessfully() {
        String csvContent = """
                sku,titulo,autor,editora,categoria,preco,estoque
                LIV-01,Clean Code,Robert C. Martin,Alta Books,Tecnologia,89.90,15
                LIV-02,The Pragmatic Programmer,Andrew Hunt,Bookman,Tecnologia,95.00,20
                """;

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CatalogDto.BatchImportResult result = batchImportService.importSpreadsheet(tenantId, companyId, stream, "livros.csv");

        assertEquals(2, result.totalProcessed());
        assertEquals(2, result.successCount());
        assertEquals(0, result.failureCount());
        assertTrue(result.errors().isEmpty());

        verify(productRepository, times(2)).save(any(Product.class));
        verify(skuRepository, times(2)).save(any(Sku.class));
        verify(stockRepository, times(2)).setStockQuantity(eq(tenantId), eq(companyId), any(UUID.class), anyInt());
    }

    @Test
    void shouldIsolateRollbackOnInvalidRowAndContinue() {
        String csvContent = """
                sku,titulo,autor,editora,categoria,preco,estoque
                LIV-01,Clean Code,Robert Martin,Alta Books,Tecnologia,89.90,15
                ,Titulo Sem SKU,Autor X,Editora Y,Geral,50.00,10
                LIV-03,Refactoring,Martin Fowler,Bookman,Tecnologia,99.90,8
                """;

        InputStream stream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CatalogDto.BatchImportResult result = batchImportService.importSpreadsheet(tenantId, companyId, stream, "livros_com_erro.csv");

        assertEquals(3, result.totalProcessed());
        assertEquals(2, result.successCount());
        assertEquals(1, result.failureCount());
        assertEquals(1, result.errors().size());

        CatalogDto.BatchImportRowError error = result.errors().get(0);
        assertEquals(3, error.rowNumber()); // Linha 3 do arquivo (header=1, linha 1=2, linha 2=3)
        assertTrue(error.errorMessage().contains("obrigatória não encontrada"));

        // As linhas 1 e 3 devem ter sido importadas normalmente!
        verify(skuRepository, times(2)).save(any(Sku.class));
    }

    @Test
    void shouldImportExcelXlsxSuccessfully() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Livros");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("sku");
            header.createCell(1).setCellValue("titulo");
            header.createCell(2).setCellValue("autor");
            header.createCell(3).setCellValue("editora");
            header.createCell(4).setCellValue("categoria");
            header.createCell(5).setCellValue("preco");
            header.createCell(6).setCellValue("estoque");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("EXCEL-01");
            row1.createCell(1).setCellValue("Livro em Excel");
            row1.createCell(2).setCellValue("Autor Excel");
            row1.createCell(3).setCellValue("Editora Excel");
            row1.createCell(4).setCellValue("Assunto");
            row1.createCell(5).setCellValue(79.90);
            row1.createCell(6).setCellValue(12);

            workbook.write(out);
        }

        InputStream stream = new ByteArrayInputStream(out.toByteArray());
        CatalogDto.BatchImportResult result = batchImportService.importSpreadsheet(tenantId, companyId, stream, "catalogo.xlsx");

        assertEquals(1, result.totalProcessed());
        assertEquals(1, result.successCount());
        assertEquals(0, result.failureCount());
        assertTrue(result.errors().isEmpty());

        verify(skuRepository).save(any(Sku.class));
    }
}

