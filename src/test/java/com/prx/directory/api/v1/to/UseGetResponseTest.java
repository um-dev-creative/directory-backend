package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UseGetResponseTest {

    @Test
    @DisplayName("Create UseGetResponse with valid data")
    void createUseGetResponseWithValidData() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null id")
    void createUseGetResponseWithNullId() {
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(null, alias, email, firstName, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null alias")
    void createUseGetResponseWithNullAlias() {
        UUID id = UUID.randomUUID();
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, null, email, firstName, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null email")
    void createUseGetResponseWithNullEmail() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, null, firstName, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null firstName")
    void createUseGetResponseWithNullFirstName() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, null, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null middleName")
    void createUseGetResponseWithNullMiddleName() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, null, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null lastName")
    void createUseGetResponseWithNullLastName() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, null, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null gender")
    void createUseGetResponseWithNullGender() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, null, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null dateOfBirth")
    void createUseGetResponseWithNullDateOfBirth() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, gender, null, createdAt, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null createdAt")
    void createUseGetResponseWithNullCreatedAt() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, gender, dateOfBirth, null, updatedAt, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null updatedAt")
    void createUseGetResponseWithNullUpdatedAt() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, gender, dateOfBirth, createdAt, null, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(gender, response.gender());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("UseGetResponse toString method")
    void useGetResponseToString() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, firstName, middleName, lastName, gender, dateOfBirth, createdAt, updatedAt, status, roleId, applicationId);

        String expectedString = "UseGetResponse{id=" + id + ", alias='" + alias + '\'' + ", email='" + email + '\'' + ", firstName='" + firstName + '\'' + ", middleName='" + middleName + '\'' + ", lastName='" + lastName + '\'' + ", gender='" + gender + '\'' + ", dateOfBirth=" + dateOfBirth + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt + ", status='" + status + '\'' + ", roleId=" + roleId + ", applicationId=" + applicationId + '}';
        assertEquals(expectedString, response.toString());
    }
}
