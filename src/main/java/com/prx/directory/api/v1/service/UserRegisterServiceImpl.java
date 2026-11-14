package com.prx.directory.api.v1.service;

import com.prx.directory.api.v1.to.ConfirmCodeRequest;
import com.prx.directory.client.mercury.MercuryClient;
import com.prx.directory.client.mercury.MercuryTokens;
import com.prx.directory.mapper.ConfirmCodeMapper;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.prx.directory.constant.DirectoryAppConstants.MESSAGE_HEADER;

///  Service implementation for user registration-related operations.
/// @see UserRegisterService
/// @see ConfirmCodeRequest
/// @see ResponseEntity
/// @see Service
@Service
public class UserRegisterServiceImpl implements UserRegisterService {

    @Value("${prx.directory.application-id}")
    private UUID applicationId;

    private final MercuryClient mercuryClient;
    private final MercuryTokens mercuryTokens;
    private final ConfirmCodeMapper confirmCodeMapper;

    /// Constructs a new UserRegisterServiceImpl with the specified MercuryClient.
    ///
    /// @param mercuryClient the client used to interact with the Mercury service
    /// @param mercuryTokens Directory's Mercury M2M token (from its Backbone managed client)
    /// @param confirmCodeMapper the mapper used to map confirmation codes
    /// @see MercuryClient
    public UserRegisterServiceImpl(MercuryClient mercuryClient, MercuryTokens mercuryTokens, ConfirmCodeMapper confirmCodeMapper) {
        this.mercuryClient = mercuryClient;
        this.confirmCodeMapper = confirmCodeMapper;
        this.mercuryTokens = mercuryTokens;
    }

    /// Confirms the verification code for the specified user.
    ///
    /// @param sessionTokenBkd the backbone token session
    /// @param confirmCodeRequest the request object containing the user ID and verification code
    /// @return a ResponseEntity containing the response object and HTTP status
    @Override
    public ResponseEntity<Void> confirmCode(String sessionTokenBkd, ConfirmCodeRequest confirmCodeRequest) {
        try {
            var verificationRequest = confirmCodeMapper.toVerificationCodeRequest(confirmCodeRequest, applicationId);
            return mercuryTokens.withToken(mercuryToken -> mercuryClient.confirmCode(mercuryToken, verificationRequest));
        } catch (FeignException e) {
            Optional<String> message = Optional.ofNullable(e.responseHeaders().get(MESSAGE_HEADER))
                    .flatMap(h -> h.stream().findFirst());
            return ResponseEntity.status(e.status())
                    .header(MESSAGE_HEADER, message.orElse(""))
                    .build();
        }
    }
}
