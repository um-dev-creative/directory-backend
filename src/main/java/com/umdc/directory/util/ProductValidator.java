package com.umdc.directory.util;

import com.umdc.directory.constant.DirectoryAppConstants;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/// Validation of the price and image fields of a product. Each method returns the error message, or null when valid.
public final class ProductValidator {

    private ProductValidator() {
        throw new IllegalStateException("Utility class");
    }

    /// Price and currency are both null or both present; the price is >= 0 with at most two decimals; the currency is an ISO 4217 code.
    public static String validatePrice(BigDecimal price, String currency) {
        if (Objects.isNull(price) && Objects.isNull(currency)) {
            return null;
        }
        if (Objects.isNull(price) || Objects.isNull(currency)) {
            return DirectoryAppConstants.PRODUCT_PRICE_CURRENCY_TOGETHER;
        }
        if (price.signum() < 0 || price.stripTrailingZeros().scale() > DirectoryAppConstants.PRODUCT_PRICE_MAX_DECIMALS) {
            return DirectoryAppConstants.PRODUCT_INVALID_PRICE;
        }
        return isIsoCurrency(normalizeCurrency(currency)) ? null : DirectoryAppConstants.PRODUCT_INVALID_CURRENCY;
    }

    /// Currency codes are stored in upper case.
    public static String normalizeCurrency(String currency) {
        return Objects.isNull(currency) ? null : currency.trim().toUpperCase(Locale.ROOT);
    }

    public static String validateImageRef(String imageRef) {
        if (Objects.nonNull(imageRef) && imageRef.length() > DirectoryAppConstants.PRODUCT_IMAGE_REF_MAX) {
            return DirectoryAppConstants.PRODUCT_INVALID_IMAGE_REF;
        }
        return null;
    }

    private static boolean isIsoCurrency(String code) {
        if (code.length() != DirectoryAppConstants.PRODUCT_CURRENCY_LENGTH || !code.chars().allMatch(c -> c >= 'A' && c <= 'Z')) {
            return false;
        }
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
