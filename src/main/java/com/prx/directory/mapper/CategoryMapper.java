package com.prx.directory.mapper;


import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.CategoryGetResponse;
import com.prx.directory.jpa.entity.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collection;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {CategoryEntity.class, CategoryGetResponse.class},
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

}
