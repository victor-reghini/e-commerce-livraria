package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.catalog.IsbnValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - IsbnValidator")
class IsbnValidatorTest {

    @ParameterizedTest(name = "ISBN-10 válido: {0}")
    @ValueSource(strings = {
            "0-306-40615-2",
            "0306406152",
            "85-359-0277-5",
            "8535902775",
            "0-8044-2957-X",
            "080442957X",
            "0-19-852663-6"
    })
    void shouldValidateValidIsbn10(String isbn) {
        assertTrue(IsbnValidator.isValid(isbn));
    }

    @ParameterizedTest(name = "ISBN-13 válido: {0}")
    @ValueSource(strings = {
            "978-85-359-0277-8",
            "9788535902778",
            "978-0-306-40615-7",
            "9780306406157",
            "978-3-16-148410-0",
            "979-10-90636-07-1"
    })
    void shouldValidateValidIsbn13(String isbn) {
        assertTrue(IsbnValidator.isValid(isbn));
    }

    @ParameterizedTest(name = "ISBN inválido: {0}")
    @ValueSource(strings = {
            "0-306-40615-3",      // dígito verificador incorreto
            "978-85-359-0277-9",  // dígito verificador incorreto
            "12345",              // muito curto
            "97812345678901234",  // muito longo
            "977-85-359-0277-8",  // prefixo não-livro (977 é ISSN)
            "ABCDEFGHIJ",         // não numérico
            ""
    })
    void shouldRejectInvalidIsbn(String isbn) {
        assertFalse(IsbnValidator.isValid(isbn));
    }

    @Test
    void shouldReturnFalseForNull() {
        assertFalse(IsbnValidator.isValid(null));
        assertNull(IsbnValidator.clean(null));
    }

    @Test
    void shouldCleanHyphensAndWhitespaces() {
        assertEquals("9788535902778", IsbnValidator.clean(" 978-85-359-0277-8 "));
        assertEquals("080442957X", IsbnValidator.clean("0-8044-2957-x"));
    }
}

