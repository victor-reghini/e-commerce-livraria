package com.livraria.ecommerce.domain.model.catalog;

public final class IsbnValidator {

    private IsbnValidator() {
    }

    public static String clean(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s-]", "").trim().toUpperCase();
    }

    public static boolean isValid(String isbn) {
        if (isbn == null) {
            return false;
        }
        String cleaned = clean(isbn);
        if (cleaned.length() == 10) {
            return isValidIsbn10(cleaned);
        } else if (cleaned.length() == 13) {
            return isValidIsbn13(cleaned);
        }
        return false;
    }

    private static boolean isValidIsbn10(String isbn10) {
        // First 9 characters must be digits
        for (int i = 0; i < 9; i++) {
            if (!Character.isDigit(isbn10.charAt(i))) {
                return false;
            }
        }
        // Last char can be digit or 'X' (representing 10)
        char lastChar = isbn10.charAt(9);
        if (!Character.isDigit(lastChar) && lastChar != 'X') {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (isbn10.charAt(i) - '0') * (10 - i);
        }
        sum += (lastChar == 'X') ? 10 : (lastChar - '0');

        return (sum % 11) == 0;
    }

    private static boolean isValidIsbn13(String isbn13) {
        if (!isbn13.startsWith("978") && !isbn13.startsWith("979")) {
            return false;
        }
        for (int i = 0; i < 13; i++) {
            if (!Character.isDigit(isbn13.charAt(i))) {
                return false;
            }
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = isbn13.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : (digit * 3);
        }

        int checkDigit = (10 - (sum % 10)) % 10;
        int expectedCheckDigit = isbn13.charAt(12) - '0';

        return checkDigit == expectedCheckDigit;
    }
}

