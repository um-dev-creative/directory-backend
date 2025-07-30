package com.prx.directory.api.v1.service;

import com.prx.commons.general.pojo.Application;
import com.prx.commons.general.pojo.Person;
import com.prx.commons.general.pojo.Role;
import com.prx.directory.api.v1.to.*;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserGetResponse;
import com.prx.directory.jpa.entity.BusinessEntity;
import com.prx.directory.jpa.entity.CategoryEntity;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(value = {SpringExtension.class})
class BusinessServiceImplTest {

    @Mock
    UserService userService;

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private BusinessMapper businessMapper;

    @Mock
    BackboneClient backboneClient;

    @InjectMocks
    BusinessServiceImpl businessService;

    private static final String T_VALUE = "Consistently relief adidas vessel ranks rica myanmar.";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(businessService, "initialRoleId", "123e4567-e89b-12d3-a456-426614174001");
    }

    @Test
    @DisplayName("Create business successfully")
    void createBusinessSuccessfully() {
        BusinessCreateRequest request = getBusinessCreateRequest();
        BusinessEntity businessEntity = getBusinessEntity();
        Person person = new Person();
        person.setGender("M");
        person.setFirstName("John");
        person.setLastName("Connor");
        person.setBirthdate(LocalDate.parse("1984-05-12"));
        person.setMiddleName("Marcus");
        var role = new Role();
        role.setId(UUID.randomUUID());
        role.setName("Role Name");
        role.setDescription("Role Description");
        role.setActive(true);
        var roles = List.of(role);
        var applications = List.of(new Application());
        BackboneUserGetResponse backboneUserGetResponse = new BackboneUserGetResponse(
                UUID.randomUUID(),
                "Alias",
                "123456789hgf",
                "frodo@shire.com",
                "The Frod",
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                true,
                true,
                true,
                person,
                roles,
                applications
        );


        when(backboneClient.findUserById(any(UUID.class))).thenReturn(backboneUserGetResponse);
        when(businessMapper.toSource(any(BusinessCreateRequest.class))).thenReturn(businessEntity);
        when(businessRepository.save(any(BusinessEntity.class))).thenReturn(businessEntity);
        when(businessMapper.toBusinessCreateResponse(any(BusinessEntity.class)))
                .thenReturn(getBusinessCreateResponse());

        ResponseEntity<BusinessCreateResponse> response = businessService.create(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("Create business with null request")
    void createBusinessWithNullRequest() {
        ResponseEntity<BusinessCreateResponse> response = businessService.create(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Create business with duplicate name")
    void createBusinessWithDuplicateName() {
        BusinessCreateRequest request = getBusinessCreateRequest();

        when(businessMapper.toSource(any(BusinessCreateRequest.class))).thenReturn(getBusinessEntity());
        when(businessRepository.save(any(BusinessEntity.class))).thenThrow(new RuntimeException("Duplicate business name"));

        ResponseEntity<BusinessCreateResponse> response = businessService.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("Create business with invalid email")
    void createBusinessWithInvalidEmail() {
        BusinessCreateRequest request = new BusinessCreateRequest(
                "Example Business",
                "This is an example business description.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@##email.ext",
                "user@email.ext",
                "user@$$email.ext",
                "www.example.com"
        );

        ResponseEntity<BusinessCreateResponse> response = businessService.create(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    private static BusinessCreateRequest getBusinessCreateRequest() {
        return new BusinessCreateRequest(
                "Example Business",
                "This is an example business description.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@email.ext",
                "user@email.ext",
                "user@email.ext",
                "www.example.com"
        );
    }

    private static BusinessCreateResponse getBusinessCreateResponse() {
        return new BusinessCreateResponse(
                UUID.randomUUID(),
                "Example Business",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private static BusinessEntity getBusinessEntity() {
        BusinessEntity businessEntity = new BusinessEntity();
        CategoryEntity categoryEntity = new CategoryEntity();
        UserEntity userEntity = new UserEntity();
        categoryEntity.setId(UUID.randomUUID());
        userEntity.setId(UUID.randomUUID());
        businessEntity.setId(UUID.randomUUID());
        businessEntity.setName("Example Business");
        businessEntity.setDescription("This is an example business description.");
        businessEntity.setCategoryFk(categoryEntity);
        businessEntity.setUserFk(userEntity);
        businessEntity.setLastUpdate(LocalDateTime.now());
        businessEntity.setCreatedDate(LocalDateTime.now());

        return businessEntity;
    }

    @Test
    @DisplayName("Find business by ID successfully")
    void findBusinessByIdSuccessfully() {
        UUID id = UUID.randomUUID();
        BusinessEntity business = new BusinessEntity();

        when(businessRepository.findById(id)).thenReturn(Optional.of(business));
        when(businessMapper.toBusinessTO(business)).thenReturn(new BusinessTO(
                id,
                "Example Business",
                "This is an example business description.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@domain.ext",
                "user@domain.ext",
                "user@domain.ext",
                "domain.ext",
                LocalDateTime.now(),
                LocalDateTime.now()
        ));

        ResponseEntity<BusinessTO> response = businessService.findById(id);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Find business by non-existent ID")
    void findBusinessByNonExistentId() {
        UUID id = UUID.randomUUID();

        when(businessRepository.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<BusinessTO> response = businessService.findById(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Find business by null ID")
    void findBusinessByNullId() {
        ResponseEntity<BusinessTO> response = businessService.findById(null);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Find businesses by user ID successfully")
    void findBusinessesByUserIdSuccessfully() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<BusinessEntity> businessPage = new PageImpl<>(Collections.singletonList(new BusinessEntity()));
        BusinessTO businessTO = new BusinessTO(UUID.randomUUID(),
                "Example Business",
                "This is an example business description.",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user@domain.ext",
                "user1@domain.ext",
                "user1@domain.ext",
                "domain.ext",
                LocalDateTime.now(),
                LocalDateTime.now());
        when(businessRepository.findByUserEntityFk(any(UserEntity.class), any(Pageable.class))).thenReturn(businessPage);
        when(businessMapper.toBusinessTO(any(BusinessEntity.class))).thenReturn(businessTO);

        ResponseEntity<Page<BusinessTO>> response = businessService.findByUserId(userId, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Find businesses by user ID with empty result")
    void findBusinessesByUserIdWithEmptyResult() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<BusinessEntity> businessPage = new PageImpl<>(Collections.emptyList());

        when(businessRepository.findByUserEntityFk(any(UserEntity.class), any(Pageable.class))).thenReturn(businessPage);

        ResponseEntity<Page<BusinessTO>> response = businessService.findByUserId(userId, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getTotalElements());
    }

    @Test
    @DisplayName("Delete business successfully as owner")
    void deleteBusinessSuccessfullyAsOwner() {
        UUID businessId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BusinessEntity businessEntity = new BusinessEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        businessEntity.setId(businessId);
        businessEntity.setUserFk(userEntity);
        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(userId);

            when(businessRepository.findById(businessId)).thenReturn(Optional.of(businessEntity));
            when(businessRepository.countByUserId(any(UUID.class))).thenReturn(0);
            when(userService.findUser(anyString(), any(UUID.class))).thenReturn(ResponseEntity.ok(getUserResponse(userId)));
            when(userService.update(any(UUID.class), any(PutUserRequest.class))).thenReturn(ResponseEntity.ok().build());
            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, T_VALUE);
            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("Delete business forbidden when not owner")
    void deleteBusinessForbiddenWhenNotOwner() {
        UUID businessId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BusinessEntity businessEntity = new BusinessEntity();
        UserEntity userEntity = new UserEntity();
        userEntity.setId(ownerId);
        businessEntity.setId(businessId);
        businessEntity.setUserFk(userEntity);
        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(UUID.randomUUID());

            when(businessRepository.findById(businessId)).thenReturn(Optional.of(businessEntity));
            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, T_VALUE);
            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("Delete business not found")
    void deleteBusinessNotFound() {
        UUID businessId = UUID.randomUUID();
        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(UUID.randomUUID());

            when(businessRepository.findById(businessId)).thenReturn(Optional.empty());
            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, T_VALUE);
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    @DisplayName("Delete business internal server error")
    void deleteBusinessInternalServerError() {
        UUID businessId = UUID.randomUUID();
        try (MockedStatic<JwtUtil> mockedStatic = Mockito.mockStatic(JwtUtil.class)) {
            mockedStatic.when(() -> JwtUtil.getUidFromToken(anyString())).thenReturn(UUID.randomUUID());

            when(businessRepository.findById(businessId)).thenThrow(new RuntimeException("DB error"));
            ResponseEntity<Void> response = businessService.deleteBusiness(businessId, T_VALUE);
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    private static GetUserResponse getUserResponse(UUID userId) {
        String profileImageRef = "LTHB/3171803c-bb08-4eb0-8821-b6ebe5948f48-250723104035.jpg";
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

        return new GetUserResponse(userId, alias, email, firstName, middleName, lastName, displayName, profileImageRef,
                phoneId, phoneNumber, Collections.emptySet(), dateOfBirth, createdAt, updatedAt, notificationEmail, notificationSms, privacyDataOutActive, status, roleId, applicationId);
    }
}
