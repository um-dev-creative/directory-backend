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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(displayName, response.displayName());
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
        String phone = "(+1) 4167489302";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String displayName = "Alias Name";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(null, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String displayName = "Alias Name";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, null, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String displayName = "Alias Name";
        String gender = "Male";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        UseGetResponse response = new UseGetResponse(id, alias, null, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, null, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, null, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, null, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                null, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, null, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, null, updatedAt, notificationEmail, notificationEmail, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, null, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

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
        String phone = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacy = true;
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

        UseGetResponse response = new UseGetResponse(id, alias, email, phone, firstName, middleName, lastName, displayName,
                gender, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacy, status, roleId, applicationId);

        String expectedString = "UseGetResponse{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", notificationEmail=" + notificationEmail +
                ", notificationSms=" + notificationSms +
                ", privacyDataOutActive=" + privacy +
                ", status='" + status + '\'' +
                ", roleId=" + roleId +
                ", applicationId=" + applicationId +
                '}';
        assertEquals(expectedString, response.toString());
    }
}
