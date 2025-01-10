package com.prx.directory.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.ProductCreateRequest;
import com.prx.directory.api.v1.to.ProductCreateResponse;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/// Mapper for the entity {@link ProductEntity} and its DTO {@link ProductCreateRequest} and {@link ProductCreateResponse}.
/// This interface extends JpaRepository to provide CRUD operations for BusinessEntity.
@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {ProductEntity.class, ProductCreateRequest.class, ProductCreateResponse.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface ProductMapper {

    /// The method maps the fields of the ProductTO object to the fields of the ProductEntity object.
    ///
    /// @param productTO The ProductTO object to convert.
    /// @return The converted ProductEntity object.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastUpdate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "categoryFk", expression = "java(getCategory(productTO.categoryId()))")
    ProductEntity toProductEntity(ProductCreateRequest productTO);

    /// The method maps the fields of the ProductTO object to the fields of the ProductEntity object.
    ///
    /// @param productEntity The ProductTO object to convert.
    /// @return The converted Product
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "createdDate", source = "createdDate")
    @Mapping(target = "lastUpdate", source = "lastUpdate")
    @Mapping(target = "categoryId", source = "categoryFk.id")
    ProductCreateResponse toProductCreateResponse(ProductEntity productEntity);

    /// Converts a BusinessEntity object to a BusinessCreateResponse object.
    ///
    /// @param categoryId The BusinessEntity object to convert.
    /// @return The converted BusinessCreateResponse object.
    default CategoryEntity getCategory(UUID categoryId){
        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(categoryId);
        return categoryEntity;
    }
}
