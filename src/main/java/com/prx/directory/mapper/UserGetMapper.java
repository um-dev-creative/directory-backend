package com.prx.directory.mapper;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Contact;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.UseGetResponse;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import org.mapstruct.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {BackboneUserGetResponse.class, UseGetResponse.class}
)
@MapperConfig(
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserGetMapper {

    /// Maps a BackboneUserGetResponse to a UseGetResponse.
    ///
    /// @param backboneUserGetResponse the BackboneUserGetResponse to map
    /// @return the UseGetResponse
    /// @see BackboneUserGetResponse
    /// @see UseGetResponse
    /// @see UserGetMapper
    @Mapping(target = "id", source = "id")
    @Mapping(target = "alias", source = "alias")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "active")
    @Mapping(target = "gender", source = "person.gender")
    @Mapping(target = "firstName", source = "person.firstName")
    @Mapping(target = "middleName", source = "person.middleName")
    @Mapping(target = "lastName", source = "person.lastName")
    @Mapping(target = "updatedAt", source = "lastUpdate")
    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "dateOfBirth", source = "person.birthdate")
    @Mapping(target = "roleId", expression = "java(getRoleId(backboneUserGetResponse))")
    @Mapping(target = "applicationId", expression = "java(getApplicationId(backboneUserGetResponse))")
    @Mapping(target = "phone", expression = "java(getPhone(backboneUserGetResponse.person()))")
    UseGetResponse fromBackbone(BackboneUserGetResponse backboneUserGetResponse);

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
            return person.getContacts().stream().filter(contact -> contact.getContactType().getName().equalsIgnoreCase("phone")).findFirst().orElse(new Contact()).getContent();
        }
        return "";
    }

}
