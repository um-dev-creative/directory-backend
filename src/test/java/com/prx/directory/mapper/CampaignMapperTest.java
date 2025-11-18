package com.prx.directory.mapper;

import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.CategoryEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CampaignMapperTest {

    private final CampaignMapper mapper = new CampaignMapperImpl();

    @Test
    void testEntityToTO() {
        CampaignEntity e = new CampaignEntity();
        UUID id = UUID.randomUUID();
        e.setId(id);
        e.setName("Camp");
        e.setDescription("Desc");
        e.setStartDate(LocalDateTime.parse("2025-01-01T00:00:00"));
        e.setEndDate(LocalDateTime.parse("2025-12-31T23:59:59"));
        e.setActive(true);
        CategoryEntity c = new CategoryEntity();
        c.setId(UUID.randomUUID());
        e.setCategoryFk(c);
        BusinessEntity b = new BusinessEntity();
        b.setId(UUID.randomUUID());
        e.setBusinessFk(b);
        e.setCreatedDate(LocalDateTime.now());
        e.setLastUpdate(LocalDateTime.now());
        e.setDiscount(BigDecimal.valueOf(12.5));

        CampaignTO to = mapper.toTO(e);
        assertNotNull(to);
        assertEquals(BigDecimal.valueOf(12.5), to.discount());
        assertEquals(id, to.id());
        assertEquals("Camp", to.name());
        assertEquals("Desc", to.description());
        assertEquals(e.getStartDate(), to.startDate());
        assertEquals(e.getEndDate(), to.endDate());
        assertEquals(b.getId(), to.businessId());
        assertEquals(c.getId(), to.categoryId());
    }

    @Test
    void testTOToEntity() {
        CampaignTO to = new CampaignTO(null, "N", "D", LocalDateTime.parse("2025-01-01T00:00:00"),
                LocalDateTime.parse("2025-12-31T00:00:00"), UUID.randomUUID(), UUID.randomUUID(), null, null, BigDecimal.valueOf(5.0), true);
        CampaignEntity e = mapper.toEntity(to);
        assertNotNull(e);
        assertEquals(BigDecimal.valueOf(5.0), e.getDiscount());
        assertEquals("N", e.getName());
        assertEquals("D", e.getDescription());
        assertEquals(to.startDate(), e.getStartDate());
        assertEquals(to.endDate(), e.getEndDate());
        assertNotNull(e.getCategoryFk());
        assertNotNull(e.getBusinessFk());
        assertEquals(to.categoryId(), e.getCategoryFk().getId());
        assertEquals(to.businessId(), e.getBusinessFk().getId());
    }
}
