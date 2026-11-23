package com.umdc.directory.util;

import com.umdc.directory.constant.DirectoryAppConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ProductValidator")
class ProductValidatorTest {

    @Test
    @DisplayName("no price and no currency is valid; one without the other is not")
    void pair() {
        assertNull(ProductValidator.validatePrice(null, null));
        assertEquals(DirectoryAppConstants.PRODUCT_PRICE_CURRENCY_TOGETHER, ProductValidator.validatePrice(BigDecimal.ONE, null));
        assertEquals(DirectoryAppConstants.PRODUCT_PRICE_CURRENCY_TOGETHER, ProductValidator.validatePrice(null, "USD"));
    }

    @Test
    @DisplayName("price must be >= 0 with at most two decimals (trailing zeros do not count)")
    void price() {
        assertNull(ProductValidator.validatePrice(BigDecimal.ZERO, "USD"));
        assertNull(ProductValidator.validatePrice(new BigDecimal("10.50"), "USD"));
        assertNull(ProductValidator.validatePrice(new BigDecimal("10.500"), "USD"));
        assertNull(ProductValidator.validatePrice(new BigDecimal("1E+3"), "USD"));
        assertEquals(DirectoryAppConstants.PRODUCT_INVALID_PRICE, ProductValidator.validatePrice(new BigDecimal("-0.01"), "USD"));
        assertEquals(DirectoryAppConstants.PRODUCT_INVALID_PRICE, ProductValidator.validatePrice(new BigDecimal("0.001"), "USD"));
    }

    @Test
    @DisplayName("currency is a 3-letter ISO 4217 code, case-insensitive")
    void currency() {
        assertNull(ProductValidator.validatePrice(BigDecimal.ONE, "usd"));
        assertNull(ProductValidator.validatePrice(BigDecimal.ONE, " COP "));
        for (String bad : new String[]{"US", "USDD", "US1", "ZZZ", "", "€€€"}) {
            assertEquals(DirectoryAppConstants.PRODUCT_INVALID_CURRENCY, ProductValidator.validatePrice(BigDecimal.ONE, bad), bad);
        }
    }

    @Test
    @DisplayName("normalizeCurrency upper-cases and trims, null stays null")
    void normalize() {
        assertEquals("USD", ProductValidator.normalizeCurrency(" usd "));
        assertNull(ProductValidator.normalizeCurrency(null));
    }

    @Test
    @DisplayName("imageRef up to 512 characters")
    void imageRef() {
        assertNull(ProductValidator.validateImageRef(null));
        assertNull(ProductValidator.validateImageRef("x".repeat(512)));
        assertEquals(DirectoryAppConstants.PRODUCT_INVALID_IMAGE_REF, ProductValidator.validateImageRef("x".repeat(513)));
    }

    @Test
    @DisplayName("utility class cannot be instantiated")
    void utility() throws ReflectiveOperationException {
        var c = ProductValidator.class.getDeclaredConstructor();
        c.setAccessible(true);
        var e = assertThrows(java.lang.reflect.InvocationTargetException.class, c::newInstance);
        assertEquals(IllegalStateException.class, e.getCause().getClass());
    }
}
