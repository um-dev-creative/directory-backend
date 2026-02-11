package com.prx.directory.mapper;

import com.prx.commons.general.pojo.Contact;
import com.prx.commons.general.pojo.ContactType;
import com.prx.commons.general.pojo.Person;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.client.backbone.to.BackboneUserCreateRequest;
import com.prx.directory.client.backbone.to.BackboneUserCreateResponse;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(
        // Specifies that the mapper should be a Spring bean.
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {BackboneUserCreateRequest.class, UserCreateRequest.class}
)
@MapperConfig(
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        // Specifies that the mapper should fail if there are any unmapped properties.
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserCreateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "alias", source = "alias")
    @Mapping(target = "applicationId", source = "applicationId")
    @Mapping(target = "email", source = "userCreateRequest.email")
    @Mapping(target = "password", source = "userCreateRequest.password")
    @Mapping(target = "person", expression = "java(getPerson(userCreateRequest))")
    BackboneUserCreateRequest toBackbone(UserCreateRequest userCreateRequest, UUID applicationId, UUID roleId, String alias);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alias", source = "alias")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "applicationId", source = "applicationId")
    UserCreateResponse fromBackbone(BackboneUserCreateResponse backboneUserCreateResponse);

    default Person getPerson(UserCreateRequest userCreateRequest) {
        var person = new Person();
        person.setFirstName(userCreateRequest.firstname());
        person.setLastName(userCreateRequest.lastname());
        person.setBirthdate(userCreateRequest.dateOfBirth());
        person.setGender("N");
        person.setMiddleName("");

        if(!userCreateRequest.phoneNumber().isBlank()) {
            var contact  = new Contact();
            var contactType = new ContactType();
            contactType.setId(UUID.fromString("61ed9501-bee2-4391-8376-91307ae02a48"));
            contact.setContent(userCreateRequest.phoneNumber());
            contact.setActive(true);
            contact.setContactType(contactType);
            person.setContacts(List.of(contact));
        }

        return person;
    }

}
