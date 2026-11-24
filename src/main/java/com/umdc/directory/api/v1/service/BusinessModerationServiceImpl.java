package com.umdc.directory.api.v1.service;

import com.umdc.directory.api.v1.to.BusinessModerationTO;
import com.umdc.directory.api.v1.to.VerificationConfirmRequest;
import com.umdc.directory.client.backbone.BackboneClient;
import com.umdc.directory.client.mercury.MercuryErrors;
import com.umdc.directory.client.mercury.MercuryThrottledException;
import com.umdc.directory.client.mercury.MercuryUnavailableException;
import com.umdc.directory.config.ModerationSettings;
import com.umdc.directory.constant.ModerationStatus;
import com.umdc.directory.jpa.entity.BusinessEntity;
import com.umdc.directory.jpa.repository.BusinessRepository;
import com.umdc.directory.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.umdc.directory.constant.DirectoryAppConstants.BUSINESS_NOT_FOUND;
import static com.umdc.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;

/// Moderation (S8) and ownership verification. The code is 9 digits, valid for 15 minutes, one e-mail per minute, 5 wrong tries; only its
/// SHA-256 is stored and it is never logged. The e-mail goes through Mercury ([MercuryVerificationService]).
@Service
public class BusinessModerationServiceImpl implements BusinessModerationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessModerationServiceImpl.class);
    static final Duration CODE_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    static final int MAX_ATTEMPTS = 5;
    private static final Pattern CODE_FORMAT = Pattern.compile("\\d{9}");
    private static final int CODE_BOUND = 1_000_000_000;

    private final BusinessRepository businessRepository;
    private final BackboneClient backboneClient;
    private final MercuryVerificationService mercuryVerification;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /// @param businessRepository  the businesses
    /// @param backboneClient      source of the owner's e-mail
    /// @param mercuryVerification sends the code through Mercury
    @Autowired
    public BusinessModerationServiceImpl(BusinessRepository businessRepository, BackboneClient backboneClient,
                                         MercuryVerificationService mercuryVerification) {
        this(businessRepository, backboneClient, mercuryVerification, Clock.systemDefaultZone());
    }

    BusinessModerationServiceImpl(BusinessRepository businessRepository, BackboneClient backboneClient,
                                  MercuryVerificationService mercuryVerification, Clock clock) {
        this.businessRepository = businessRepository;
        this.backboneClient = backboneClient;
        this.mercuryVerification = mercuryVerification;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ResponseEntity<Void> requestVerification(UUID businessId, String token) {
        var checked = ownedBusiness(businessId, token);
        if (checked.failure() != null) {
            return failure(checked.failure());
        }
        var business = checked.business();
        var now = LocalDateTime.now(clock);
        if (Objects.nonNull(business.getVerificationCodeHash()) && Objects.nonNull(business.getVerificationExpiresAt())) {
            var nextAllowed = business.getVerificationExpiresAt().minus(CODE_TTL).plus(RESEND_COOLDOWN);
            if (nextAllowed.isAfter(now)) {
                var wait = Math.max(1, Duration.between(now, nextAllowed).toSeconds());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header(HttpHeaders.RETRY_AFTER, String.valueOf(wait))
                        .header(MESSAGE_ERROR_HEADER, "A code was sent a moment ago. Wait before asking for another.").build();
            }
        }
        com.umdc.directory.client.backbone.to.BackboneUserGetResponse owner;
        try {
            owner = backboneClient.findUserById(business.getUserFk());
        } catch (RuntimeException e) {
            LOGGER.warn("The owner of business {} could not be read: {}", businessId, e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(MESSAGE_ERROR_HEADER, "The owner could not be read. Try again later.").build();
        }
        if (Objects.isNull(owner) || Objects.isNull(owner.email()) || owner.email().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .header(MESSAGE_ERROR_HEADER, "The owner has no e-mail to send the code to.").build();
        }
        var code = String.format(Locale.ROOT, "%09d", random.nextInt(CODE_BOUND));
        try {
            mercuryVerification.sendCode(owner.id(), owner.email(), owner.displayName(), code);
        } catch (MercuryThrottledException | MercuryUnavailableException e) {
            return MercuryErrors.toResponse(e);
        }
        business.setVerificationCodeHash(hash(businessId, code));
        business.setVerificationExpiresAt(now.plus(CODE_TTL));
        business.setVerificationAttempts(0);
        businessRepository.save(business);
        return ResponseEntity.accepted().build();
    }

    @Override
    @Transactional
    public ResponseEntity<Void> confirmVerification(UUID businessId, VerificationConfirmRequest request, String token) {
        var checked = ownedBusiness(businessId, token);
        if (checked.failure() != null) {
            return failure(checked.failure());
        }
        if (Objects.isNull(request) || Objects.isNull(request.code()) || !CODE_FORMAT.matcher(request.code().trim()).matches()) {
            return error(HttpStatus.BAD_REQUEST, "The code must be 9 digits.");
        }
        var business = checked.business();
        if (Objects.isNull(business.getVerificationCodeHash()) || Objects.isNull(business.getVerificationExpiresAt())) {
            return error(HttpStatus.CONFLICT, "No verification code is pending. Ask for a new one.");
        }
        if (!business.getVerificationExpiresAt().isAfter(LocalDateTime.now(clock))) {
            clearCode(business);
            businessRepository.save(business);
            return error(HttpStatus.GONE, "The verification code expired. Ask for a new one.");
        }
        if (business.getVerificationAttempts() >= MAX_ATTEMPTS) {
            return error(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong codes. Ask for a new one.");
        }
        var expected = business.getVerificationCodeHash().getBytes(StandardCharsets.UTF_8);
        var given = hash(businessId, request.code().trim()).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, given)) {
            business.setVerificationAttempts(business.getVerificationAttempts() + 1);
            businessRepository.save(business);
            return error(HttpStatus.BAD_REQUEST, "Invalid verification code.");
        }
        clearCode(business);
        business.setVerified(true);
        businessRepository.save(business);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BusinessModerationTO> status(UUID businessId, String token) {
        var checked = ownedBusiness(businessId, token);
        if (checked.failure() != null) {
            return failure(checked.failure());
        }
        return ResponseEntity.ok(toTO(checked.business()));
    }

    @Override
    @Transactional
    public ResponseEntity<BusinessModerationTO> approve(UUID businessId, String token) {
        return moderate(businessId, token, ModerationStatus.APPROVED);
    }

    @Override
    @Transactional
    public ResponseEntity<BusinessModerationTO> reject(UUID businessId, String token) {
        return moderate(businessId, token, ModerationStatus.REJECTED);
    }

    private ResponseEntity<BusinessModerationTO> moderate(UUID businessId, String token, ModerationStatus target) {
        if (Objects.isNull(token) || token.isBlank()) {
            return error(HttpStatus.UNAUTHORIZED, "Session token is required.");
        }
        if (!ModerationAdmin.isAdmin(token)) {
            return error(HttpStatus.FORBIDDEN, "Only a moderator can do this.");
        }
        var found = businessRepository.findById(businessId);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, BUSINESS_NOT_FOUND);
        }
        var business = found.get();
        business.setModerationStatus(target);
        business.setLastUpdate(LocalDateTime.now(clock));
        businessRepository.save(business);
        LOGGER.info("Business {} moderated to {}", businessId, target);
        return ResponseEntity.ok(toTO(business));
    }

    private record Checked(BusinessEntity business, ResponseEntity<Void> failure) {
    }

    /// Loads the business and checks the caller owns it; `failure` holds the 401, 404 or 403 answer otherwise.
    private Checked ownedBusiness(UUID businessId, String token) {
        var userId = JwtUtil.getUidFromToken(token);
        if (Objects.isNull(userId)) {
            return new Checked(null, error(HttpStatus.UNAUTHORIZED, "Session token is required."));
        }
        var found = businessRepository.findById(businessId);
        if (found.isEmpty()) {
            return new Checked(null, error(HttpStatus.NOT_FOUND, BUSINESS_NOT_FOUND));
        }
        if (!userId.equals(found.get().getUserFk())) {
            return new Checked(null, error(HttpStatus.FORBIDDEN, "Only the owner can do this."));
        }
        return new Checked(found.get(), null);
    }

    @SuppressWarnings("unchecked")
    private static <T> ResponseEntity<T> failure(ResponseEntity<Void> failure) {
        return (ResponseEntity<T>) failure;
    }

    private static <T> ResponseEntity<T> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).header(MESSAGE_ERROR_HEADER, message).build();
    }

    private static void clearCode(BusinessEntity business) {
        business.setVerificationCodeHash(null);
        business.setVerificationExpiresAt(null);
        business.setVerificationAttempts(null);
    }

    private static BusinessModerationTO toTO(BusinessEntity business) {
        return new BusinessModerationTO(business.getId(), business.getModerationStatus(), business.isVerified());
    }

    private static String hash(UUID businessId, String code) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest((businessId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
