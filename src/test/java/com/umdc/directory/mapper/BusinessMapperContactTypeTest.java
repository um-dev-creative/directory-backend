package com.umdc.directory.mapper;

import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.entity.DigitalContactEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("BusinessMapper - contact type names come from the map served by Backbone")
class BusinessMapperContactTypeTest {

    private final BusinessMapper mapper = new BusinessMapperImpl();

    private BusinessEntity business(UUID emlId, UUID unknownId) {
        var business = new BusinessEntity();
        business.setId(UUID.randomUUID());
        business.setUserFk(UUID.randomUUID());
        var eml = new DigitalContactEntity();
        eml.setContactTypeFk(emlId);
        eml.setContent("a@b.com");
        var unknown = new DigitalContactEntity();
        unknown.setContactTypeFk(unknownId);
        unknown.setContent("x");
        var noType = new DigitalContactEntity();
        noType.setContent("y");
        business.setDigitalContacts(new HashSet<>(java.util.List.of(eml, unknown, noType)));
        return business;
    }

    @Test
    @DisplayName("email is resolved by id through the name map; the owner id is the plain user column")
    void resolvesByName() {
        var emlId = UUID.randomUUID();
        var business = business(emlId, UUID.randomUUID());

        var to = mapper.toBusinessTO(business, Map.of(emlId, "EML"));

        assertEquals("a@b.com", to.email());
        assertNull(to.website());
        assertEquals(business.getUserFk(), to.userId());
    }

    @Test
    @DisplayName("without names (Backbone down, nothing cached) the contacts are simply not mapped")
    void emptyNames() {
        var to = mapper.toBusinessTO(business(UUID.randomUUID(), UUID.randomUUID()));
        assertNull(to.email());
        assertNull(to.customerServiceEmail());
    }

    @Test
    @DisplayName("create request keeps the user id as plain UUID")
    void createRequestUserId() {
        var userId = UUID.randomUUID();
        var entity = mapper.toSource(new com.umdc.directory.api.v1.to.BusinessCreateRequest("n", "d", UUID.randomUUID(), userId,
                null, null, null, null));
        assertEquals(userId, entity.getUserFk());
    }
}
