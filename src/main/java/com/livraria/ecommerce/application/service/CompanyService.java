package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.in.CompanyUseCase;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyService implements CompanyUseCase {

    private final CompanyRepositoryPort companyRepository;
    private final AuditUseCase auditUseCase;

    @Autowired
    public CompanyService(CompanyRepositoryPort companyRepository,
                          @Autowired(required = false) AuditUseCase auditUseCase) {
        this.companyRepository = companyRepository;
        this.auditUseCase = auditUseCase;
    }

    public CompanyService(CompanyRepositoryPort companyRepository) {
        this(companyRepository, null);
    }

    @Override
    @Transactional
    public Company createCompany(CreateCompanyCommand command) {
        if (companyRepository.existsByCodeAndTenantId(command.code(), command.tenantId())) {
            throw new BusinessException("Já existe uma empresa/filial cadastrada com o código: " + command.code());
        }

        Company company = Company.create(
                command.tenantId(),
                command.code(),
                command.name(),
                command.tradeName(),
                command.cnpj(),
                command.stateRegistration(),
                command.municipalRegistration(),
                command.taxRegime(),
                command.headquarters(),
                command.address()
        );

        Company saved = companyRepository.save(company);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("COMPANY_CREATED", "COMPANY", saved.getId().toString(), Map.of(
                    "code", saved.getCode(),
                    "tradeName", saved.getTradeName(),
                    "cnpj", saved.getCnpj()
            ));
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyById(UUID id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Empresa/Filial não encontrada com ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompanyByCode(String code) {
        return companyRepository.findByCodeAndTenantId(code, null)
                .orElseThrow(() -> new EntityNotFoundException("Empresa/Filial não encontrada com código: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> listCompaniesByTenant(UUID tenantId) {
        return companyRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional
    public Company updateCompany(UpdateCompanyCommand command) {
        Company company = getCompanyById(command.id());

        company.update(
                command.name(),
                command.tradeName(),
                command.cnpj(),
                command.stateRegistration(),
                command.municipalRegistration(),
                command.taxRegime(),
                command.headquarters(),
                command.address()
        );

        Company saved = companyRepository.save(company);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("COMPANY_UPDATED", "COMPANY", saved.getId().toString(), Map.of(
                    "tradeName", saved.getTradeName(),
                    "cnpj", saved.getCnpj()
            ));
        }
        return saved;
    }

    @Override
    @Transactional
    public void toggleCompanyStatus(UUID id, boolean active) {
        Company company = getCompanyById(id);
        if (active) {
            company.activate();
        } else {
            company.deactivate();
        }
        companyRepository.save(company);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("COMPANY_STATUS_TOGGLED", "COMPANY", id.toString(), Map.of(
                    "active", active,
                    "tradeName", company.getTradeName()
            ));
        }
    }
}

