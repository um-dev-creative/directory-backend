package com.umdev.directory.mapper;

import com.umdev.commons.services.config.mapper.MapperAppConfig;
import com.umdev.directory.api.v1.to.FavoriteResponse;
import com.umdev.directory.constant.FavoriteType;
import com.umdev.directory.jpa.entity.UserFavoriteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Objects;
import java.util.UUID;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        config = MapperAppConfig.class
)
public interface FavoriteMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "type", expression = "java(getType(entity))")
    @Mapping(target = "itemId", expression = "java(getItemId(entity))")
    @Mapping(target = "userId", expression = "java(getUserId(entity))")
    @Mapping(target = "createdDate", source = "createdAt")
    @Mapping(target = "updatedDate", source = "updatedAt")
    @Mapping(target = "active", source = "active")
    FavoriteResponse toResponse(UserFavoriteEntity entity);

    default String getType(UserFavoriteEntity entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        if (Objects.nonNull(entity.getBusiness())) {
            return FavoriteType.STORE.name();
        } else if (Objects.nonNull(entity.getProduct())) {
            return FavoriteType.PRODUCT.name();
        } else if (Objects.nonNull(entity.getCampaign())) {
            return FavoriteType.OFFER.name();
        }
        return null;
    }

    default UUID getItemId(UserFavoriteEntity entity) {
        if (Objects.isNull(entity)) {
            return null;
        }
        if (Objects.nonNull(entity.getBusiness())) {
            return entity.getBusiness().getId();
        } else if (Objects.nonNull(entity.getProduct())) {
            return entity.getProduct().getId();
        } else if (Objects.nonNull(entity.getCampaign())) {
            return entity.getCampaign().getId();
        }
        return null;
    }

    default UUID getUserId(UserFavoriteEntity entity) {
        if (Objects.isNull(entity) || Objects.isNull(entity.getUser())) {
            return null;
        }
        return entity.getUser().getId();
    }
}
