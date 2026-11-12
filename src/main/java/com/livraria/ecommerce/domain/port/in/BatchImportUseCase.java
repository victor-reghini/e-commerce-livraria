package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;

import java.io.InputStream;
import java.util.UUID;

public interface BatchImportUseCase {

    CatalogDto.BatchImportResult importSpreadsheet(UUID tenantId, UUID companyId, InputStream inputStream, String fileName);
}

