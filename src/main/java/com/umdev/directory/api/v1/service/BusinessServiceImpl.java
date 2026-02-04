package com.umdev.directory.api.v1.service;

import com.umdev.directory.client.backbone.BackboneClient;
import com.umdev.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.umdev.directory.constant.ContactTypeKey;
import com.umdev.directory.jpa.entity.CategoryEntity;
import com.umdev.directory.jpa.entity.ContactTypeEntity;
import com.umdev.directory.jpa.entity.DigitalContactEntity;
import com.umdev.directory.jpa.entity.UserEntity;
import com.umdev.directory.jpa.entity.BusinessEntity;
import com.umdev.directory.jpa.repository.BusinessRepository;
import com.umdev.directory.jpa.repository.CategoryRepository;
import com.umdev.directory.jpa.repository.DigitalContactRepository;
import com.umdev.directory.mapper.BusinessMapper;
import com.umdev.directory.util.JwtUtil;
import com.umdev.directory.api.v1.to.*;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.umdev.directory.constant.DirectoryAppConstants.MESSAGE_HEADER;
import static com.umdev.directory.constant.RoleKey.LH_STANDARD;

/// Service implementation for business-related operations.
@Service
public class BusinessServiceImpl implements BusinessService {

    private final UserService userService;
    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final DigitalContactRepository digitalContactRepository;
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
    public BusinessServiceImpl(UserService userService, BusinessRepository businessRepository,
                               CategoryRepository categoryRepository,
                               DigitalContactRepository digitalContactRepository,
                               BusinessMapper businessMapper, BackboneClient backboneClient) {
        this.userService = userService;
        this.businessRepository = businessRepository;
        this.categoryRepository = categoryRepository;
        this.digitalContactRepository = digitalContactRepository;
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
        var result = businessRepository.findBusinessWithDigitalContactsById(id);
        return result.map(businessEntity ->
                        ResponseEntity.ok(businessMapper.toBusinessTO(businessEntity)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResponseEntity<Set<BusinessTO>> findByUserId(@NotNull UUID userId) {
        var businessPage = businessRepository.findByUserEntityFk(userId).stream()
                .map(businessMapper::toBusinessTO).collect(Collectors.toSet());
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
    public ResponseEntity<BusinessUpdateResponse> update(@NotNull UUID id, @NotNull BusinessUpdateRequest request) {
        if (Objects.isNull(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Business ID is required.")
                    .build();
        }
        if (Objects.isNull(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header(MESSAGE_HEADER, "Business update request is required.")
                    .build();
        }

        // Find existing business
        var businessOpt = businessRepository.findBusinessWithDigitalContactsById(id);
        if (businessOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(MESSAGE_HEADER, "Business not found.")
                    .build();
        }

        var existingBusiness = businessOpt.get();
        boolean updated = false;

        // Update basic fields
        if (Objects.nonNull(request.name()) && !request.name().isBlank()) {
            // Check if name already exists for another business
            var businessWithName = businessRepository.findByName(request.name().trim());
            if (businessWithName.isPresent() && !businessWithName.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .header(MESSAGE_HEADER, "Business with this name already exists.")
                        .build();
            }
            existingBusiness.setName(request.name().trim());
            updated = true;
        }

        if (Objects.nonNull(request.description())) {
            existingBusiness.setDescription(request.description());
            updated = true;
        }

        // Validate and update category if provided
        if (Objects.nonNull(request.categoryId())) {
            if (!categoryRepository.existsById(request.categoryId())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(MESSAGE_HEADER, "Category not found.")
                        .build();
            }
            CategoryEntity categoryEntity = new CategoryEntity();
            categoryEntity.setId(request.categoryId());
            existingBusiness.setCategoryFk(categoryEntity);
            updated = true;
        }

        // Validate and update user if provided
        if (Objects.nonNull(request.id())) {
            try {
                var user = backboneClient.findUserById(request.id());
                if (Objects.isNull(user)) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .header(MESSAGE_HEADER, "User not found.")
                            .build();
                }
                UserEntity userEntity = new UserEntity();
                userEntity.setId(request.id());
                existingBusiness.setUserFk(userEntity);
                updated = true;
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .header(MESSAGE_HEADER, "User not found.")
                        .build();
            }
        }

        // Validate website if provided
        if (Objects.nonNull(request.website())) {
            if (!request.website().isEmpty() && !isValidUrl(request.website())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, "Website URL format is invalid.")
                        .build();
            }
            updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.WBH, request.website());
        }

        // Update email contacts
        if (Objects.nonNull(request.email())) {
            updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.EML, request.email());
        }

        if (Objects.nonNull(request.customerServiceEmail())) {
            updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.SCE, request.customerServiceEmail());
        }

        if (Objects.nonNull(request.orderManagementEmail())) {
            updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.MEC, request.orderManagementEmail());
        }

        // Update lastUpdate timestamp if any field was updated
        if (updated) {
            existingBusiness.setLastUpdate(LocalDateTime.now());
        }

        var savedBusiness = businessRepository.save(existingBusiness);
        return ResponseEntity.ok(new BusinessUpdateResponse(savedBusiness.getLastUpdate()));
    }

    /**
     * Updates or creates a digital contact for the business.
     *
     * @param business the business entity
     * @param contactTypeKey the contact type key
     * @param content the contact content
     * @return true if updated, false otherwise
     */
    private boolean updateOrCreateDigitalContact(BusinessEntity business,
                                                 ContactTypeKey contactTypeKey, String content) {
        if (Objects.isNull(content)) {
            return false;
        }

        var digitalContacts = business.getDigitalContacts();
        if (Objects.isNull(digitalContacts)) {
            digitalContacts = new HashSet<>();
            business.setDigitalContacts(digitalContacts);
        }

        // Find existing digital contact
        var existingContact = digitalContacts.stream()
                .filter(dc -> contactTypeKey.toString().equals(dc.getContactType().getName()))
                .findFirst();

        if (existingContact.isPresent()) {
            // Update existing contact
            var contact = existingContact.get();
            if (!content.equals(contact.getContent())) {
                contact.setContent(content);
                contact.setLastUpdate(LocalDateTime.now());
                digitalContactRepository.save(contact);
                return true;
            }
            return false;
        } else {
            // Create new digital contact
            DigitalContactEntity newContact = new DigitalContactEntity();
            newContact.setContent(content);
            newContact.setBusiness(business);
            newContact.setCreatedDate(LocalDateTime.now());
            newContact.setLastUpdate(LocalDateTime.now());

            // Set contact type
            ContactTypeEntity contactType = new ContactTypeEntity();
            contactType.setName(contactTypeKey.toString());
            newContact.setContactType(contactType);

            digitalContacts.add(newContact);
            digitalContactRepository.save(newContact);
            return true;
        }
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
            var countResult = businessRepository.countByUserId(userId);
            if(countResult <= 0 ) {
                return updateUserRole(token, userId);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public Set<UUID> findIdByUserId(UUID id) {
        if(Objects.isNull(id)) {
            return Collections.emptySet();
        }
        return businessRepository.findIdCollectionByUserId(id);
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
