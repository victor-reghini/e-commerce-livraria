package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.company.TaxRegime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompanyTest {

    @Test
    @DisplayName("Deve criar empresa filial válida com código em maiúsculo e CNPJ sanitizado")
    void shouldCreateValidCompany() {
        UUID tenantId = UUID.randomUUID();
        Address address = new Address("Rua Augusta", "500", "Sala 2", "Consolação", "São Paulo", "SP", "01305-000");

        Company company = Company.create(
                tenantId,
                "  filial-sp-01  ",
                "Livraria SP Comércio de Livros Ltda",
                "Livraria Paulista Filial",
                "12.345.678/0002-71",
                "987654321",
                "12345",
                TaxRegime.SIMPLES_NACIONAL,
                false,
                address
        );

        assertNotNull(company.getId());
        assertEquals(tenantId, company.getTenantId());
        assertEquals("FILIAL-SP-01", company.getCode());
        assertEquals("Livraria SP Comércio de Livros Ltda", company.getName());
        assertEquals("Livraria Paulista Filial", company.getTradeName());
        assertEquals("12345678000271", company.getCnpj());
        assertEquals("987654321", company.getStateRegistration());
        assertEquals(TaxRegime.SIMPLES_NACIONAL, company.getTaxRegime());
        assertFalse(company.isHeadquarters());
        assertTrue(company.isActive());
        assertEquals("Rua Augusta", company.getAddress().street());
    }

    @Test
    @DisplayName("Deve atualizar dados da empresa e endereço")
    void shouldUpdateCompanyData() {
        UUID tenantId = UUID.randomUUID();
        Company company = Company.create(
                tenantId,
                "LOJA-02",
                "Nome Antigo",
                null,
                null,
                null,
                null,
                TaxRegime.SIMPLES_NACIONAL,
                false,
                null
        );

        Address newAddress = new Address("Av. Rio Branco", "100", null, "Centro", "Rio de Janeiro", "RJ", "20040-002");
        company.update("Novo Nome", "Nome Fantasia", "99.888.777/0001-66", "111222", "333444",
                TaxRegime.LUCRO_PRESUMIDO, true, newAddress);

        assertEquals("Novo Nome", company.getName());
        assertEquals("Nome Fantasia", company.getTradeName());
        assertEquals("99888777000166", company.getCnpj());
        assertEquals(TaxRegime.LUCRO_PRESUMIDO, company.getTaxRegime());
        assertTrue(company.isHeadquarters());
        assertEquals("Rio de Janeiro", company.getAddress().city());
    }

    @Test
    @DisplayName("Deve permitir ativar e desativar filial")
    void shouldToggleCompanyActive() {
        Company company = Company.create(
                UUID.randomUUID(),
                "STAND-FEIRA",
                "Stand Feira do Livro",
                null,
                null,
                null,
                null,
                TaxRegime.MEI,
                false,
                null
        );

        assertTrue(company.isActive());
        company.deactivate();
        assertFalse(company.isActive());
        company.activate();
        assertTrue(company.isActive());
    }

    @Test
    @DisplayName("Deve lançar exceção quando dados obrigatórios forem nulos")
    void shouldThrowExceptionWhenRequiredFieldsNull() {
        assertThrows(NullPointerException.class, () ->
                Company.create(null, "COD", "Nome", null, null, null, null, null, false, null)
        );

        assertThrows(NullPointerException.class, () ->
                Company.create(UUID.randomUUID(), null, "Nome", null, null, null, null, null, false, null)
        );

        assertThrows(NullPointerException.class, () ->
                Company.create(UUID.randomUUID(), "COD", null, null, null, null, null, null, false, null)
        );
    }
}

