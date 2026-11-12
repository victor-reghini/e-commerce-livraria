package com.livraria.ecommerce.application.dto;

import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.TaxRegime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "Razão social / Nome é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String name,

        String tradeName,

        @Pattern(regexp = "^\\d{14}$|^$", message = "CNPJ deve conter 14 dígitos numéricos")
        String cnpj,

        String stateRegistration,
        String municipalRegistration,
        TaxRegime taxRegime,
        boolean headquarters,
        Address address
) {}

