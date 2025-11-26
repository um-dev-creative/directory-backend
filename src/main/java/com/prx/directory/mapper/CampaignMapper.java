package com.prx.directory.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.CampaignTO;
import com.prx.directory.api.v1.to.OfferTO;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CampaignEntity;
import com.prx.directory.jpa.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


@Mapper(config = MapperAppConfig.class)
public interface CampaignMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "businessId", source = "businessFk.id")
    @Mapping(target = "startDate", source = "startDate")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "discount", source = "discount")
    OfferTO toOfferTO(CampaignEntity campaignEntity);

    @Mapping(target = "businessId", source = "businessFk.id")
    @Mapping(target = "categoryId", source = "categoryFk.id")
    @Mapping(target = "categoryName", source = "categoryFk.name")
    @Mapping(target = "terms", source = "terms")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "discount", source = "discount")
    CampaignTO toTO(CampaignEntity entity);

    @Mapping(target = "businessFk", source = "businessId", qualifiedByName = "toBusinessEntity")
    @Mapping(target = "categoryFk", source = "categoryId", qualifiedByName = "toCategoryEntity")
    @Mapping(target = "discount", source = "discount")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastUpdate", ignore = true)
    CampaignEntity toEntity(CampaignTO to);

    @Named("toCategoryEntity")
    default CategoryEntity toCategoryEntity(java.util.UUID id) {
        if (id == null) return null;
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
        return c;
    }

    @Named("toBusinessEntity")
    default BusinessEntity toBusinessEntity(java.util.UUID id) {
        if (id == null) return null;
        BusinessEntity b = new BusinessEntity();
        b.setId(id);
        return b;
    }
}
