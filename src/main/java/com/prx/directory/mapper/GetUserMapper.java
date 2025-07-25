package com.prx.directory.mapper;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Contact;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.GetUserResponse;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import org.mapstruct.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {BackboneUserGetResponse.class, GetUserResponse.class}
)
@MapperConfig(
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface GetUserMapper {

    /// Maps a BackboneUserGetResponse to a UseGetResponse.
    ///
    /// @param backboneUserGetResponse the BackboneUserGetResponse to map
    /// @return the UseGetResponse
    /// @see BackboneUserGetResponse
    /// @see GetUserResponse
    /// @see GetUserMapper
    @Mapping(target = "id", source = "backboneUserGetResponse.id")
    @Mapping(target = "alias", source = "backboneUserGetResponse.alias")
    @Mapping(target = "email", source = "backboneUserGetResponse.email")
    @Mapping(target = "firstName", source = "backboneUserGetResponse.person.firstName")
    @Mapping(target = "middleName", source = "backboneUserGetResponse.person.middleName")
    @Mapping(target = "lastName", source = "backboneUserGetResponse.person.lastName")
    @Mapping(target = "profileImageRef", source = "profileImageRef")
    @Mapping(target = "updatedAt", source = "backboneUserGetResponse.lastUpdate")
    @Mapping(target = "createdAt", source = "backboneUserGetResponse.createdDate")
    @Mapping(target = "dateOfBirth", source = "backboneUserGetResponse.person.birthdate")
    @Mapping(target = "phoneId", expression = "java(getContactId(backboneUserGetResponse.person()))")
    @Mapping(target = "phoneNumber", expression = "java(getPhone(backboneUserGetResponse.person()))")
    @Mapping(target = "status", source = "backboneUserGetResponse.active")
    @Mapping(target = "roleId", expression = "java(getRoleId(backboneUserGetResponse))")
    @Mapping(target = "applicationId", expression = "java(getApplicationId(backboneUserGetResponse))")
    GetUserResponse fromBackbone(BackboneUserGetResponse backboneUserGetResponse, String profileImageRef);

    default UUID getRoleId(BackboneUserGetResponse backboneUserGetResponse) {
        List<Role> roles = backboneUserGetResponse.roles();
        if (Objects.nonNull(roles) && !roles.isEmpty()) {
            return roles.stream().map(Role::getId).filter(Objects::nonNull).findFirst().orElse(null);
        }
        return null;
    }

    default UUID getApplicationId(BackboneUserGetResponse backboneUserGetResponse) {
        List<Application> applications = backboneUserGetResponse.applications();
        if (Objects.nonNull(applications) && !applications.isEmpty()) {
            return applications.stream().map(Application::getId).filter(Objects::nonNull).findFirst().orElse(null);
        }
        return null;
    }

    default String getPhone(Person person) {
        if (Objects.nonNull(person)) {
            return person.getContacts().stream()
                    .filter(contact -> contact.getContactType().getName().equalsIgnoreCase("Phone"))
                    .findFirst().orElse(new Contact()).getContent();
        }
        return "";
    }

    default UUID getContactId(Person person) {
        if (Objects.nonNull(person)) {
            return person.getContacts().stream()
                    .filter(contact -> contact.getContactType().getName().equalsIgnoreCase("Phone"))
                    .findFirst().orElse(new Contact()).getId();
        }
        return null;
    }

}
