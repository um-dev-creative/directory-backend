package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.*;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.backbone.to.BackboneUserUpdateRequest;
import com.umdc.directory.client.backbone.to.ContactType;
import com.umdc.directory.constant.ContactTypeKey;
import com.umdc.directory.jpa.entity.CategoryEntity;
import com.umdc.directory.jpa.entity.DigitalContactEntity;
import com.umdc.directory.jpa.entity.UserEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.jpa.repository.CategoryRepository;
import com.umdc.directory.jpa.repository.DigitalContactRepository;
import com.umdc.directory.mapper.BusinessMapper;
import com.umdc.directory.util.JwtUtil;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.umdc.directory.constant.DirectoryAppConstants.MESSAGE_HEADER;
import static com.umdc.directory.constant.RoleKey.LH_STANDARD;

/** Service implementation for business-related operations. */
@Service
@SuppressWarnings("PMD.GodClass") // Class has many responsibilities; decomposed some logic but further refactor is recommended
public class BusinessServiceImpl implements BusinessService {

    private static final Set<ContactTypeKey> SUPPORTED_CONTACT_TYPES = EnumSet.of(
            ContactTypeKey.EML,
            ContactTypeKey.WBH,
            ContactTypeKey.SCE,
            ContactTypeKey.MEC
    );

    private final UserService userService;
    private final BusinessRepository businessRepository;
    private final CategoryRepository categoryRepository;
    private final DigitalContactRepository digitalContactRepository;
    private final BusinessMapper businessMapper;
    private final BackboneClient backboneClient;
    private final Map<ContactTypeKey, UUID> contactTypeIds = new ConcurrentHashMap<>();

    @Value("${umdc.directory.application-id}")
    private UUID applicationId;
    @Value("${umdc.directory.role-id}")
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
    public ResponseEntity<BusinessCreateResponse> create(BusinessCreateRequest requestedBusiness, String token) {
        var tokenUserId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(MESSAGE_HEADER, "Session token is required.")
                    .build();
        }
        final var BAD_REQUEST = getBusinessCreateResponseResponseEntity(requestedBusiness);
        if (BAD_REQUEST != null) return BAD_REQUEST;

        // The owner is always the user of the token; a different userId in the body is impersonation.
        if (Objects.nonNull(requestedBusiness.userId()) && !tokenUserId.equals(requestedBusiness.userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header(MESSAGE_HEADER, "User ID does not match the session token.")
                    .build();
        }
        final var businessCreateRequest = Objects.isNull(requestedBusiness.userId())
                ? new BusinessCreateRequest(requestedBusiness.name(), requestedBusiness.description(),
                requestedBusiness.categoryId(), tokenUserId, requestedBusiness.email(),
                requestedBusiness.customerServiceEmail(), requestedBusiness.orderManagementEmail(),
                requestedBusiness.website())
                : requestedBusiness;

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

    private void loadContactTypeIds() {
        List<ContactType> contactTypeCollection;

        try {
            contactTypeCollection = Optional.ofNullable(backboneClient.findAllContactType()).orElse(Collections.emptyList());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Contact types are not available.", ex);
        }

        var resolvedContactTypeIds = new EnumMap<ContactTypeKey, UUID>(ContactTypeKey.class);
        for (var contactType : contactTypeCollection) {
            if (Objects.isNull(contactType) || Objects.isNull(contactType.id()) || Objects.isNull(contactType.name())) {
                continue;
            }
            var key = mapContactTypeKey(contactType.name());
            if (Objects.nonNull(key)) {
                resolvedContactTypeIds.put(key, contactType.id());
            }
        }

        contactTypeIds.clear();
        contactTypeIds.putAll(resolvedContactTypeIds);
    }

    private ContactTypeKey mapContactTypeKey(String contactTypeName) {
        var normalized = contactTypeName.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "EML", "EMAIL" -> ContactTypeKey.EML;
            case "WBH", "WEBSITE" -> ContactTypeKey.WBH;
            case "SCE", "CUSTOMER_SERVICE_EMAIL" -> ContactTypeKey.SCE;
            case "MEC", "ORDER_MANAGEMENT_EMAIL" -> ContactTypeKey.MEC;
            default -> null;
        };
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
        // A missing userId is allowed (the token user is used); the all-zero UUID is still invalid.
        if (Objects.nonNull(businessCreateRequest.userId()) &&
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
    public ResponseEntity<BusinessUpdateResponse> update(@NotNull UUID id, @NotNull BusinessUpdateRequest request, String token) {
        var tokenUserId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(tokenUserId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(MESSAGE_HEADER, "Session token is required.")
                    .build();
        }
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
        if (Objects.isNull(existingBusiness.getUserFk()) || !tokenUserId.equals(existingBusiness.getUserFk().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .header(MESSAGE_HEADER, "Only the owner can update the business.")
                    .build();
        }
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
            try {
                updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.WBH, request.website());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, ex.getMessage())
                        .build();
            }
        }

        // Update email contacts
        if (Objects.nonNull(request.email())) {
            try {
                updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.EML, request.email());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, ex.getMessage())
                        .build();
            }
        }

        if (Objects.nonNull(request.customerServiceEmail())) {
            try {
                updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.SCE, request.customerServiceEmail());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, ex.getMessage())
                        .build();
            }
        }

        if (Objects.nonNull(request.orderManagementEmail())) {
            try {
                updated |= updateOrCreateDigitalContact(existingBusiness, ContactTypeKey.MEC, request.orderManagementEmail());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .header(MESSAGE_HEADER, ex.getMessage())
                        .build();
            }
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
    private boolean updateOrCreateDigitalContact(com.umdc.directory.jpa.entity.BusinessEntity business,
                                                  ContactTypeKey contactTypeKey, String content) {
        if (Objects.isNull(content)) {
            return false;
        }

        var contactTypeId = resolveContactTypeId(contactTypeKey);
        var digitalContacts = business.getDigitalContacts();
        if (Objects.isNull(digitalContacts)) {
            digitalContacts = new HashSet<>();
            business.setDigitalContacts(digitalContacts);
        }

        // Find existing digital contact by stored FK; fallback to relation id for backward compatibility.
        var existingContact = digitalContacts.stream()
                .filter(dc -> Objects.equals(contactTypeId, dc.getContactTypeFk())
                        || (Objects.nonNull(dc.getContactType()) && Objects.equals(contactTypeId, dc.getContactType().getId())))
                .findFirst();

        if (existingContact.isPresent()) {
            var contact = existingContact.get();
            if (!content.equals(contact.getContent())) {
                contact.setContent(content);
                contact.setLastUpdate(LocalDateTime.now());
                contact.setContactTypeFk(contactTypeId);
                digitalContactRepository.save(contact);
                return true;
            }
            return false;
        } else {
            // Create new digital contact when business has no contacts or this contact type is missing.
            DigitalContactEntity newContact = new DigitalContactEntity();
            newContact.setContent(content);
            newContact.setBusiness(business);
            newContact.setCreatedDate(LocalDateTime.now());
            newContact.setLastUpdate(LocalDateTime.now());
            newContact.setContactTypeFk(contactTypeId);

            digitalContacts.add(newContact);
            digitalContactRepository.save(newContact);
            return true;
        }
    }

    private UUID resolveContactTypeId(ContactTypeKey contactTypeKey) {
        var configuredContactTypeId = mapContactTypeId(contactTypeKey);
        if (Objects.isNull(configuredContactTypeId)) {
            throw new IllegalArgumentException("Contact type not found for key " + contactTypeKey);
        }
        return configuredContactTypeId;
    }

    private UUID mapContactTypeId(ContactTypeKey contactTypeKey) {
        if (!contactTypeIds.keySet().containsAll(SUPPORTED_CONTACT_TYPES)) {
            loadContactTypeIds();
        }
        return contactTypeIds.get(contactTypeKey);
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

    /**
     * A helper method designed to perform a specific, singular functionality
     * within the larger context of an application. The exact purpose of the method
     * should be determined by the implementing logic.
     * <p>
     * This method is intended to encapsulate reusable behavior to reduce code
     * duplication and improve modularity. It may handle common operations,
     * validations, transformations, or utility tasks.
     * <p>
     * Note: Further details about the behavior or its parameters, return type,
     * and exceptions (if applicable) should be provided upon implementation.
     */
    private boolean isValidUrl(String url) {
        return url.matches("^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$");
    }
}
