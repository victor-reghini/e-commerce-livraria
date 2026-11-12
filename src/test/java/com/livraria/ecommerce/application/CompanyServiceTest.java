package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.service.CompanyService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.company.TaxRegime;
import com.livraria.ecommerce.domain.port.in.CompanyUseCase;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepositoryPort companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private UUID tenantId;
    private CompanyUseCase.CreateCompanyCommand createCommand;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        createCommand = new CompanyUseCase.CreateCompanyCommand(
                tenantId,
                "LOJA-1",
                "Livraria Central Ltda",
                "Livraria Central",
                "12345678000190",
                "123456",
                null,
                TaxRegime.SIMPLES_NACIONAL,
                true,
                new Address("Rua 1", "10", null, "Centro", "SP", "SP", "01000-000")
        );
    }

    @Test
    @DisplayName("Deve cadastrar empresa filial com sucesso")
    void shouldCreateCompanySuccessfully() {
        when(companyRepository.existsByCodeAndTenantId("LOJA-1", tenantId)).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        Company company = companyService.createCompany(createCommand);

        assertNotNull(company);
        assertEquals("LOJA-1", company.getCode());
        assertEquals("Livraria Central Ltda", company.getName());
        verify(companyRepository).save(any(Company.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando o código já existir no mesmo tenant")
    void shouldThrowExceptionWhenCodeAlreadyExists() {
        when(companyRepository.existsByCodeAndTenantId("LOJA-1", tenantId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> companyService.createCompany(createCommand));
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar filial por ID")
    void shouldGetCompanyById() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.create(tenantId, "MATRIZ", "Matriz", null, null, null, null, null, true, null);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));

        Company found = companyService.getCompanyById(companyId);
        assertNotNull(found);
        assertEquals("MATRIZ", found.getCode());
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando ID não existir")
    void shouldThrowExceptionWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> companyService.getCompanyById(companyId));
    }

    @Test
    @DisplayName("Deve listar filiais por tenant")
    void shouldListCompaniesByTenant() {
        Company c1 = Company.create(tenantId, "LOJA-1", "Loja 1", null, null, null, null, null, true, null);
        Company c2 = Company.create(tenantId, "LOJA-2", "Loja 2", null, null, null, null, null, false, null);

        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(c1, c2));

        List<Company> list = companyService.listCompaniesByTenant(tenantId);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Deve atualizar dados da filial com sucesso")
    void shouldUpdateCompany() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.create(tenantId, "LOJA-1", "Nome Antigo", null, null, null, null, null, false, null);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyUseCase.UpdateCompanyCommand updateCmd = new CompanyUseCase.UpdateCompanyCommand(
                companyId,
                "Novo Nome Ltda",
                "Novo Fantasia",
                "12345678000199",
                "999888",
                "777666",
                TaxRegime.LUCRO_PRESUMIDO,
                true,
                null
        );

        Company updated = companyService.updateCompany(updateCmd);
        assertEquals("Novo Nome Ltda", updated.getName());
        assertTrue(updated.isHeadquarters());
        assertEquals(TaxRegime.LUCRO_PRESUMIDO, updated.getTaxRegime());
    }

    @Test
    @DisplayName("Deve alternar status de ativação da filial")
    void shouldToggleCompanyStatus() {
        UUID companyId = UUID.randomUUID();
        Company company = Company.create(tenantId, "FEIRA", "Feira", null, null, null, null, null, false, null);

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        companyService.toggleCompanyStatus(companyId, false);
        assertFalse(company.isActive());

        companyService.toggleCompanyStatus(companyId, true);
        assertTrue(company.isActive());
    }
}

