package com.prx.directory.api.v1.to;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GetUserResponseTest {

    @Test
    @DisplayName("Create UseGetResponse with valid data")
    void createUseGetResponseWithValidData() {
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        String displayName = "Alias Name";
        UUID phoneId = UUID.randomUUID();
        String phoneNumber = "(+1) 4167489302";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
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
        String displayName = "Alias Name";
        UUID phoneId = UUID.randomUUID();
        String phoneNumber = "(+1) 4167489302";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(null, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
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
        String displayName = "Alias Name";
        UUID phoneId = UUID.randomUUID();
        String phoneNumber = "(+1) 4167489302";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, null, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
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
        String displayName = "Alias Name";
        UUID phoneId = UUID.randomUUID();
        String phoneNumber = "(+1) 4167489302";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, null, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null firstName")
    void createUseGetResponseWithNullFirstName() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String middleName = "Middle";
        String lastName = "Last";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, null, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null middleName")
    void createUseGetResponseWithNullMiddleName() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String lastName = "Last";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, null, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null lastName")
    void createUseGetResponseWithNullLastName() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, null, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null phoneId")
    void createUseGetResponseWithNullPhoneId() {
        String phoneNumber = "(+1) 4167489302";
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
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

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                null, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null phoneNumber")
    void createUseGetResponseWithNullPhoneNumber() {
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
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

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, null, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null dateOfBirth")
    void createUseGetResponseWithNullDateOfBirth() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, null, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(notificationEmail, response.notificationEmail());
        assertEquals(notificationSms, response.notificationSms());
        assertEquals(privacyDataOutActive, response.privacyDataOutActive());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null createdAt")
    void createUseGetResponseWithNullCreatedAt() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime updatedAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, null, updatedAt, notificationEmail, notificationEmail, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(updatedAt, response.updatedAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("Create UseGetResponse with null updatedAt")
    void createUseGetResponseWithNullUpdatedAt() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
        UUID id = UUID.randomUUID();
        String alias = "userAlias";
        String email = "user@example.com";
        String firstName = "First";
        String middleName = "Middle";
        String lastName = "Last";
        LocalDate dateOfBirth = LocalDate.now();
        LocalDateTime createdAt = LocalDateTime.now();
        boolean status = true;
        UUID roleId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, null, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(alias, response.alias());
        assertEquals(email, response.email());
        assertEquals(firstName, response.firstName());
        assertEquals(middleName, response.middleName());
        assertEquals(lastName, response.lastName());
        assertEquals(displayName, response.displayName());
        assertEquals(phoneId, response.phoneId());
        assertEquals(phoneNumber, response.phoneNumber());
        assertEquals(dateOfBirth, response.dateOfBirth());
        assertEquals(createdAt, response.createdAt());
        assertEquals(status, response.status());
        assertEquals(roleId, response.roleId());
        assertEquals(applicationId, response.applicationId());
    }

    @Test
    @DisplayName("UseGetResponse toString method")
    void useGetResponseToString() {
        String phoneNumber = "(+1) 4167489302";
        UUID phoneId = UUID.randomUUID();
        String displayName = "Alias Name";
        boolean notificationEmail = true;
        boolean notificationSms = true;
        boolean privacyDataOutActive = true;
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

        GetUserResponse response = new GetUserResponse(id, alias, email, firstName, middleName, lastName, displayName,
                phoneId, phoneNumber, dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);

        String expectedString = "UseGetResponse{" +
                "id=" + id +
                ", alias='" + alias + '\'' +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", middleName='" + middleName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", phoneId=" + phoneId +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", notificationEmail=" + notificationEmail +
                ", notificationSms=" + notificationSms +
                ", privacyDataOutActive=" + privacyDataOutActive +
                ", status='" + status + '\'' +
                ", roleId=" + roleId +
                ", applicationId=" + applicationId +
                '}';
        assertEquals(expectedString, response.toString());
    }
}
