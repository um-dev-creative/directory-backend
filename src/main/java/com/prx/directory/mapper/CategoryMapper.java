package com.prx.directory.mapper;


import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.CategoryCreateRequest;
import com.prx.directory.api.v1.to.CategoryCreateResponse;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import com.prx.directory.jpa.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.Collection;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {CategoryGetResponse.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface CategoryMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "lastUpdate", source = "lastUpdate")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdDate", source = "createdDate")
    @Mapping(target = "categoryParentId", source = "categoryParentFk.id")
    CategoryGetResponse toCategoryGetResponse(CategoryEntity categoryEntity);

    Collection<CategoryGetResponse> toCategoryGetResponse(Collection<CategoryEntity> categoryEntities);

    // Manual mapping from create request to entity to avoid MapStruct incorrect generation for entities.
    default CategoryEntity toCategoryEntity(CategoryCreateRequest request) {
        if (request == null) return null;
        CategoryEntity entity = new CategoryEntity();
        entity.setName(request.name());
        entity.setDescription(request.description() == null ? "" : request.description());
        entity.setActive(request.active() == null ? true : request.active());
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastUpdate(LocalDateTime.now());
        if (request.categoryParentId() != null) {
            CategoryEntity parent = new CategoryEntity();
            parent.setId(request.categoryParentId());
            entity.setCategoryParentFk(parent);
        }
        return entity;
    }

    default CategoryCreateResponse toCategoryCreateResponse(CategoryEntity entity) {
        if (entity == null) return null;
        return new CategoryCreateResponse(entity.getId(), entity.getCreatedDate());
    }

}
