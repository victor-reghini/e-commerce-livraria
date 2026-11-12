package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.port.in.CompanyUseCase;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CompanyService implements CompanyUseCase {

    private final CompanyRepositoryPort companyRepository;

    public CompanyService(CompanyRepositoryPort companyRepository) {
        this.companyRepository = companyRepository;
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

        return companyRepository.save(company);
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

        return companyRepository.save(company);
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
    }
}

