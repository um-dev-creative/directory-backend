package com.prx.directory.mapper;


import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.LinkBusinessProductRequest;
import com.prx.directory.api.v1.to.LinkBusinessProductResponse;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.BusinessProductEntity;
import com.prx.directory.jpa.entity.BusinessProductEntityId;
import com.prx.directory.jpa.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/// Mapper for the entity {@link BusinessProductEntity} and its DTO {@link LinkBusinessProductRequest} and {@link LinkBusinessProductResponse}.
/// This interface extends JpaRepository to provide CRUD operations for BusinessEntity.
@Mapper(
        // Specifies that the mapper should be a Spring bean.
        uses = {BusinessProductEntity.class, LinkBusinessProductRequest.class, LinkBusinessProductResponse.class},
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface BusinessProductMapper {

    /// Converts a LinkBusinessProductRequest object to a BusinessProductEntity object.
    ///
    /// @param linkBusinessProductRequest The LinkBusinessProductRequest object to convert.
    /// @return The converted BusinessProductEntity object.
    @Mapping(target = "id", expression = "java(getBusinessProductEntityId(linkBusinessProductRequest))")
    @Mapping(target = "business", expression = "java(getBusiness(linkBusinessProductRequest.businessId()))")
    @Mapping(target = "product", expression = "java(getProduct(linkBusinessProductRequest.productId()))")
    @Mapping(target = "createdDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastUpdate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "active", expression = "java(true)")
    BusinessProductEntity toSource(LinkBusinessProductRequest linkBusinessProductRequest);

    /// Converts a BusinessProductEntity object to a LinkBusinessProductResponse object.
    ///
    /// @param businessProductEntity The BusinessProductEntity object to convert.
    /// @return The converted LinkBusinessProductResponse object.
    @Mapping(target = "businessId", source = "business.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "lastUpdate", source = "lastUpdate")
    @Mapping(target = "active", source = "active")
    LinkBusinessProductResponse toLinkBusinessProductResponse(BusinessProductEntity businessProductEntity);

    default BusinessEntity getBusiness(UUID businessId) {
        var businessEntity = new BusinessEntity();
        businessEntity.setId(businessId);
        return businessEntity;
    }

    default ProductEntity getProduct(UUID productId) {
        var productEntity = new ProductEntity();
        productEntity.setId(productId);
        return productEntity;
    }

    default BusinessProductEntityId getBusinessProductEntityId(LinkBusinessProductRequest linkBusinessProductRequest) {
        var businessProductEntityId = new BusinessProductEntityId();

        businessProductEntityId.setBusinessId(linkBusinessProductRequest.businessId());
        businessProductEntityId.setProductId(linkBusinessProductRequest.productId());
        return businessProductEntityId;
    }

}
