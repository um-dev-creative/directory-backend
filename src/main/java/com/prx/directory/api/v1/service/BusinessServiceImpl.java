package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.BusinessCreateRequest;
import com.prx.directory.api.v1.to.BusinessCreateResponse;
import com.prx.directory.api.v1.to.BusinessTO;
import com.prx.directory.api.v1.to.PutUserRequest;
import com.prx.directory.client.backbone.BackboneClient;
import com.prx.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.prx.directory.jpa.entity.UserEntity;
import com.prx.directory.jpa.repository.BusinessRepository;
import com.prx.directory.mapper.BusinessMapper;
import com.prx.directory.util.JwtUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_HEADER;
import static com.prx.directory.constant.RoleKey.LH_STANDARD;

/// Service implementation for business-related operations.
@Service
public class BusinessServiceImpl implements BusinessService {

    private final UserService userService;
    private final BusinessRepository businessRepository;
    private final BusinessMapper businessMapper;
    private final BackboneClient backboneClient;
    @Value("${prx.directory.application-id}")
    private UUID applicationId;
    @Value("${prx.directory.role-id}")
    private String initialRoleId;

    /// Constructs a new BusinessServiceImpl with the specified BusinessRepository and BusinessMapper.
    ///
    /// @param businessRepository the repository used to access business data
    /// @param businessMapper     the mapper used to convert between business-related objects
    public BusinessServiceImpl(UserService userService, BusinessRepository businessRepository, BusinessMapper businessMapper, BackboneClient backboneClient) {
        this.userService = userService;
        this.businessRepository = businessRepository;
        this.businessMapper = businessMapper;
        this.backboneClient = backboneClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<BusinessCreateResponse> create(BusinessCreateRequest businessCreateRequest) {
        final var BAD_REQUEST = getBusinessCreateResponseResponseEntity(businessCreateRequest);
        if (BAD_REQUEST != null) return BAD_REQUEST;

        // Check if business with the same name already exists
        var existingBusiness = businessRepository.findByName(businessCreateRequest.name());
        if (existingBusiness.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(MESSAGE_HEADER, "Business exist")
                    .build();
        }

        try {
            var backboneUserGetResponse = backboneClient.findUserById(businessCreateRequest.userId());
            if (Objects.nonNull(backboneUserGetResponse)) {
                var business = businessMapper.toSource(businessCreateRequest);
                var savedBusiness = businessRepository.save(business);

                var standardRoleFounded = backboneUserGetResponse.roles().stream().filter(role -> role.getName().equals(LH_STANDARD.name())).findFirst();

                if(standardRoleFounded.isPresent()){
                    UUID roleId = UUID.fromString("9a232260-a2e3-4990-b062-b6966efb25f8");
                    BackboneUserUpdateRequest backboneUserUpdateRequest = new BackboneUserUpdateRequest(applicationId,
                            backboneUserGetResponse.notificationEmail(), backboneUserGetResponse.notificationSms(), backboneUserGetResponse.privacyDataOutActive(), List.of(roleId));
                    backboneClient.userPartialUpdate(backboneUserGetResponse.id(), backboneUserUpdateRequest);
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(businessMapper.toBusinessCreateResponse(savedBusiness));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, "User not found.")
                        .build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(MESSAGE_HEADER, "A conflict occurred while creating the business.")
                    .build();
        }
    }

    private ResponseEntity<BusinessCreateResponse> getBusinessCreateResponseResponseEntity(BusinessCreateRequest businessCreateRequest) {
        // Validate UUIDs are not default/empty
        if(Objects.isNull(businessCreateRequest)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Business Create Request is null.")
                    .build();
        }
        if (Objects.isNull(businessCreateRequest.categoryId()) || businessCreateRequest.categoryId().toString().isEmpty() ||
                businessCreateRequest.categoryId().toString().equals("00000000-0000-0000-0000-000000000000")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Category ID is invalid.")
                    .build();
        }
        if (Objects.isNull(businessCreateRequest.userId()) || businessCreateRequest.userId().toString().isEmpty() ||
                businessCreateRequest.userId().toString().equals("00000000-0000-0000-0000-000000000000")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "User ID is invalid.")
                    .build();
        }
        // Validate website if present
        if(Objects.nonNull(businessCreateRequest.website()) && !businessCreateRequest.website().isEmpty()
                && !isValidUrl(businessCreateRequest.website())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, "Website URL format is invalid.")
                        .build();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<BusinessTO> findById(@NotNull UUID id) {
        var business = businessRepository.findById(id);
        return business.map(businessEntity ->
                        ResponseEntity.ok(businessMapper.toBusinessTO(businessEntity)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Page<BusinessTO>> findByUserId(@NotNull UUID userId, Pageable pageable) {
        UserEntity user = new UserEntity();
        user.setId(userId);
        var businessPage = businessRepository.findByUserEntityFk(user, pageable)
                .map(businessMapper::toBusinessTO);
        return ResponseEntity.ok(businessPage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<BusinessTO> findByName(@NotNull String name) {
        if (Objects.isNull(name) || name.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Business name is required.")
                    .build();
        }

        var business = businessRepository.findByName(name.trim());
        return business.map(businessEntity ->
                        ResponseEntity.ok(businessMapper.toBusinessTO(businessEntity)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(MESSAGE_HEADER, "Business not found.")
                        .build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public ResponseEntity<Void> deleteBusiness(@NotNull UUID businessId, @NotNull String token) {
        try {
            var userId = JwtUtil.getUidFromToken(token);
            if(Objects.isNull(userId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            var businessOpt = businessRepository.findById(businessId);
            if (businessOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            var business = businessOpt.get();
            if (Objects.isNull(business.getUserFk()) || !userId.equals(business.getUserFk().getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            businessRepository.deleteById(businessId);
            var countResult = businessRepository.findByUserId(userId);
            if(countResult <= 0 ) {
                return updateUserRole(token, userId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<Void> updateUserRole(String token, UUID userId) {
        var findUserResponse = userService.findUser(token, userId);
        if(Objects.nonNull(findUserResponse) && findUserResponse.getStatusCode().is2xxSuccessful()) {
            var businessRoleId = UUID.fromString(initialRoleId);
            var currentUser = findUserResponse.getBody();
            if(Objects.nonNull(currentUser)) {
                PutUserRequest putUserRequest = new PutUserRequest(null, null, null,
                        currentUser.notificationEmail(), currentUser.notificationSms(), currentUser.privacyDataOutActive(),
                        null, null, List.of(businessRoleId), currentUser.status());
                var response = userService.update(userId, putUserRequest);
                return response.getStatusCode().equals(HttpStatus.OK) ? ResponseEntity.accepted().build() : response;
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    // Helper method to validate URL format
    private boolean isValidUrl(String url) {
        return url.matches("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$");
    }
}
