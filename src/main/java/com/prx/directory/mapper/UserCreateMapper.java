package com.prx.directory.mapper;

import com.prx.commons.pojo.Person;
import com.prx.directory.api.v1.to.UserCreateRequest;
import com.prx.directory.api.v1.to.UserCreateResponse;
import com.prx.directory.client.to.BackboneUserCreateRequest;
import com.prx.directory.client.to.BackboneUserCreateResponse;
import org.mapstruct.*;

import java.security.SecureRandom;
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

    @Mapping(target = "alias", expression = "java(aliasRandom(userCreateRequest))")
    @Mapping(target = "email", source = "userCreateRequest.email")
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "password", source = "userCreateRequest.password")
    @Mapping(target = "person", expression = "java(getPerson(userCreateRequest))")
    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "applicationId", source = "applicationId")
    BackboneUserCreateRequest toBackbone(UserCreateRequest userCreateRequest, UUID applicationId, UUID roleId);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "alias", source = "alias")
    @Mapping(target = "active", source = "active")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "roleId", source = "roleId")
    @Mapping(target = "personId", source = "personId")
    @Mapping(target = "applicationId", source = "applicationId")
    UserCreateResponse fromBackbone(BackboneUserCreateResponse backboneUserCreateResponse);

    default String aliasRandom(UserCreateRequest userCreateRequest) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[3];
        random.nextBytes(bytes);
        String username;
        if (userCreateRequest.firstname().length() >= 2 && userCreateRequest.lastname().length() >= 4) {
            username = userCreateRequest.firstname().substring(0, 1).toLowerCase().concat(userCreateRequest.lastname().toLowerCase());
        } else {
            username = userCreateRequest.firstname().toLowerCase().concat("-")
                .concat(userCreateRequest.lastname().toLowerCase()) + random.nextInt();
        }
        if(username.length() > 12) {
            return username.substring(0, 12);
        }
        return username;
    }

    default Person getPerson(UserCreateRequest userCreateRequest) {
        var person = new Person();
        person.setFirstName(userCreateRequest.firstname());
        person.setLastName(userCreateRequest.lastname());
        person.setBirthdate(userCreateRequest.dateOfBirth());
        person.setGender("N");
        person.setMiddleName("");

        return person;
    }

}
