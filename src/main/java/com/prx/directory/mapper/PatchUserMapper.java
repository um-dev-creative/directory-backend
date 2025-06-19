package com.prx.directory.mapper;

import com.prx.commons.services.config.mapper.MapperAppConfig;
import com.prx.directory.api.v1.to.PatchUserRequest;
import com.prx.directory.client.backbone.to.BackboneUserUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

/**
 * Mapper interface for converting PatchUserRequest to BackboneUserUpdateRequest.
 */
@Mapper(
        // Specifies the configuration class to use for this mapper.
        config = MapperAppConfig.class
)
public interface PatchUserMapper {

    @Mapping(target = "displayName", source = "displayName")
    @Mapping(target = "password", source = "password")
    @Mapping(target = "notificationEmail", source = "notificationEmail")
    @Mapping(target = "notificationSms", source = "notificationSms")
    @Mapping(target = "privacyDataOutActive", source = "privacyDataOutActive")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "contacts", expression = "java(mapContacts(request))")
    BackboneUserUpdateRequest toBackbone(PatchUserRequest request);

    /**
     * Helper method to map person fields from PatchUserRequest to BackboneUserUpdateRequest.Person.
     */
    default List<BackboneUserUpdateRequest.Contact> mapContacts(PatchUserRequest request) {
        return List.of(new BackboneUserUpdateRequest.Contact(
                request.contact().id(),
                request.contact().content(),
                new BackboneUserUpdateRequest.ContactType(UUID.fromString("61ed9501-bee2-4391-8376-91307ae02a48")),
                true
        ));
    }
}

