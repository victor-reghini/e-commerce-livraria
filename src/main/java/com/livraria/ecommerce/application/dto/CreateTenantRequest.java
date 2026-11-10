package com.livraria.ecommerce.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateTenantRequest(
        @NotBlank(message = "Slug do tenant é obrigatório")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug deve conter apenas letras minúsculas, números e hífens")
        @Size(min = 3, max = 64, message = "Slug deve ter entre 3 e 64 caracteres")
        String slug,

        @NotBlank(message = "Nome da empresa/livraria é obrigatório")
        @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
        String name,

        String tradeName,

        @Pattern(regexp = "^[0-9]{14}$|^$", message = "CNPJ deve conter 14 dígitos numéricos")
        String cnpj,

        @NotBlank(message = "E-mail de contato é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        String phone,

        Map<String, Object> settings,

        String primaryDomain
) {}

