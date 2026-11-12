package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.company.TaxRegime;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {

    Company createCompany(CreateCompanyCommand command);

    Company getCompanyById(UUID id);

    Company getCompanyByCode(String code);

    List<Company> listCompaniesByTenant(UUID tenantId);

    Company updateCompany(UpdateCompanyCommand command);

    void toggleCompanyStatus(UUID id, boolean active);

    record CreateCompanyCommand(
            UUID tenantId,
            String code,
            String name,
            String tradeName,
            String cnpj,
            String stateRegistration,
            String municipalRegistration,
            TaxRegime taxRegime,
            boolean headquarters,
            Address address
    ) {}

    record UpdateCompanyCommand(
            UUID id,
            String name,
            String tradeName,
            String cnpj,
            String stateRegistration,
            String municipalRegistration,
            TaxRegime taxRegime,
            boolean headquarters,
            Address address
    ) {}
}

