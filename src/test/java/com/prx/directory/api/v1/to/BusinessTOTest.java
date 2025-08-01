package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BusinessTOTest {

    @Test
    @DisplayName("BusinessTO should correctly store and return values")
    void shouldStoreAndReturnValues() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        BusinessTO businessTO = new BusinessTO(
                id, "Test Business", "Description", userId, categoryId,
                "test@example.com", "cs@example.com", "om@example.com",
                "http://example.com", now, now, false
        );

        assertEquals(id, businessTO.id());
        assertEquals("Test Business", businessTO.name());
        assertEquals("Description", businessTO.description());
        assertEquals(userId, businessTO.userId());
        assertEquals(categoryId, businessTO.categoryId());
        assertEquals("test@example.com", businessTO.email());
        assertEquals("cs@example.com", businessTO.customerServiceEmail());
        assertEquals("om@example.com", businessTO.orderManagementEmail());
        assertEquals("http://example.com", businessTO.website());
        assertEquals(now, businessTO.createdDate());
        assertEquals(now, businessTO.updatedDate());
    }

    @Test
    @DisplayName("BusinessTO toString should return correct string representation")
    void toStringShouldReturnCorrectStringRepresentation() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        BusinessTO businessTO = new BusinessTO(
                id, "Test Business", "Description", userId, categoryId,
                "test@example.com", "cs@example.com", "om@example.com",
                "http://example.com", now, now, true
        );

        String expected = "BusinessTO{" +
                "id=" + id +
                ", name='Test Business'" +
                ", description='Description'" +
                ", userId=" + userId +
                ", categoryId=" + categoryId +
                ", email='test@example.com'" +
                ", customerServiceEmail='cs@example.com'" +
                ", orderManagementEmail='om@example.com'" +
                ", website='http://example.com'" +
                ", createdDate=" + now +
                ", updatedDate=" + now +
                ", verified=" + true +
                '}';

        assertEquals(expected, businessTO.toString());
    }
}
