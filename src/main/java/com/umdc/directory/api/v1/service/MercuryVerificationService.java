package com.umdc.directory.api.v1.service;

import com.umdc.directory.client.mercury.MercuryClient;
import com.umdc.directory.client.mercury.MercuryThrottledException;
import com.umdc.directory.client.mercury.MercuryTokens;
import com.umdc.directory.client.mercury.MercuryUnavailableException;
import com.umdc.directory.client.mercury.to.VerificationCodeRequest;
import com.umdc.directory.kafka.producer.EmailMessageProducerService;
import com.umdc.directory.kafka.to.EmailMessageTO;
import com.umdc.directory.kafka.to.Recipient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Thin layer over Mercury for verification codes: sends a code by e-mail (Kafka topic Mercury reads), confirms a code at Mercury and asks
/// whether a user's code was confirmed. It never logs a code or a token. Failures of Mercury surface as [MercuryThrottledException]
/// (429, with the wait) or [MercuryUnavailableException] (503); a 401 is not retried in a loop (see [MercuryTokens]).
@Service
public class MercuryVerificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MercuryVerificationService.class);
    private static final String FROM = "support@latinhub.info";
    private static final int NOT_FOUND = 404;

    private final MercuryClient mercuryClient;
    private final MercuryTokens mercuryTokens;
    private final EmailMessageProducerService producer;

    @Value("${umdc.verification.code.template.id:}")
    private String templateId;
    @Value("${umdc.directory.application-id:}")
    private String applicationId;

    /// @param mercuryClient Mercury's Feign client
    /// @param mercuryTokens Directory's Mercury token
    /// @param producer      the Kafka producer of the e-mails Mercury sends
    public MercuryVerificationService(MercuryClient mercuryClient, MercuryTokens mercuryTokens, EmailMessageProducerService producer) {
        this.mercuryClient = mercuryClient;
        this.mercuryTokens = mercuryTokens;
        this.producer = producer;
    }

    /// Sends a verification code to an e-mail address through Mercury.
    ///
    /// @param userId      the user the code is for
    /// @param email       where to send it
    /// @param displayName name shown in the e-mail
    /// @param code        the code (never logged)
    /// @throws MercuryUnavailableException when the message cannot be queued
    public void sendCode(UUID userId, String email, String displayName, String code) {
        try {
            var message = new EmailMessageTO(templateId == null || templateId.isBlank() ? null : UUID.fromString(templateId),
                    userId, FROM, List.of(new Recipient(displayName, email, displayName)), Collections.emptyList(),
                    "Verification code", "Your verification code is in the message.", LocalDateTime.now(),
                    Map.of("vc", code, "user_name", displayName == null ? "" : displayName));
            producer.sendMessage(message);
        } catch (RuntimeException e) {
            LOGGER.error("The verification e-mail could not be queued for user {}: {}", userId, e.getClass().getSimpleName());
            throw new MercuryUnavailableException("The verification e-mail could not be sent.", e);
        }
    }

    /// Confirms a code at Mercury (`POST /api/v1/verification-code`).
    ///
    /// @param userId the user
    /// @param code   the code the user typed (never logged)
    /// @return true when Mercury accepted it, false when Mercury rejected it (a 4xx other than throttling)
    /// @throws MercuryThrottledException   when Mercury is throttling this client
    /// @throws MercuryUnavailableException when Mercury cannot be reached or answers 5xx
    public boolean verifyCode(UUID userId, String code) {
        try {
            var request = new VerificationCodeRequest(UUID.fromString(applicationId), userId, code);
            var response = mercuryTokens.withToken(token -> mercuryClient.confirmCode(token, request));
            return response != null && response.getStatusCode().is2xxSuccessful();
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                return false;
            }
            throw new MercuryUnavailableException("Mercury failed with status " + e.status() + ".", e);
        } catch (MercuryThrottledException | MercuryUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MercuryUnavailableException("Mercury could not confirm the code.", e);
        }
    }

    /// Whether the user's latest code was confirmed at Mercury.
    ///
    /// @param userId the user
    /// @return true when confirmed; false when pending or when Mercury has no code for the user (404)
    /// @throws MercuryThrottledException   when Mercury is throttling this client
    /// @throws MercuryUnavailableException when Mercury cannot be reached or answers 5xx
    public boolean isVerified(UUID userId) {
        try {
            var done = mercuryTokens.withToken(token -> mercuryClient.isVerificationCodeDone(token, userId));
            return Boolean.TRUE.equals(done);
        } catch (FeignException e) {
            if (e.status() == NOT_FOUND) {
                return false;
            }
            throw new MercuryUnavailableException("Mercury failed with status " + e.status() + ".", e);
        } catch (MercuryThrottledException | MercuryUnavailableException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new MercuryUnavailableException("Mercury could not report the verification.", e);
        }
    }
}
