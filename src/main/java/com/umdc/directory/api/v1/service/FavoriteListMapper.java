package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.FavoriteListItemTO;
import com.umdc.directory.api.v1.to.FavoriteListTO;
import com.umdc.directory.constant.FavoriteListConstants;
import com.umdc.directory.jpa.entity.FavoriteListEntity;
import com.umdc.directory.jpa.entity.FavoriteListItemEntity;
import com.umdc.directory.jpa.entity.UserFavoriteEntity;

import java.util.Objects;

/// Maps the favorite list entities to their transfer objects.
final class FavoriteListMapper {

    private FavoriteListMapper() {
        throw new IllegalStateException("Utility class");
    }

    static FavoriteListTO toTO(FavoriteListEntity e, long count) {
        return new FavoriteListTO(e.getId(), e.getName(), count, e.getCreatedAt(), e.getUpdatedAt());
    }

    static FavoriteListItemTO toItemTO(FavoriteListItemEntity i) {
        UserFavoriteEntity fav = i.getFavorite();
        if (Objects.nonNull(fav.getBusiness())) {
            var b = fav.getBusiness();
            return new FavoriteListItemTO(fav.getId(), FavoriteListConstants.TYPE_STORE, b.getId(), b.getName(), i.getCreatedAt());
        }
        var p = fav.getProduct();
        return new FavoriteListItemTO(fav.getId(), FavoriteListConstants.TYPE_PRODUCT, p.getId(), p.getName(), i.getCreatedAt());
    }
}
