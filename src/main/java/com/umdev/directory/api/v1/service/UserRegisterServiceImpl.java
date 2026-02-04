package com.umdev.directory.api.v1.service;

import com.umdev.directory.api.v1.to.ConfirmCodeRequest;
import com.umdev.directory.client.backbone.BackboneClient;
import com.umdev.directory.client.mercury.MercuryClient;
import com.umdev.directory.mapper.ConfirmCodeMapper;
import com.umdev.security.to.AuthRequest;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.umdev.directory.constant.DirectoryAppConstants.MESSAGE_HEADER;

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
    private final BackboneClient backboneClient;
    private final ConfirmCodeMapper confirmCodeMapper;

    /// Constructs a new UserRegisterServiceImpl with the specified MercuryClient.
    ///
    /// @param mercuryClient the client used to interact with the Mercury service
    /// @param confirmCodeMapper the mapper used to map confirmation codes
    /// @see MercuryClient
    public UserRegisterServiceImpl(MercuryClient mercuryClient, ConfirmCodeMapper confirmCodeMapper, BackboneClient backboneClient) {
        this.mercuryClient = mercuryClient;
        this.confirmCodeMapper = confirmCodeMapper;
        this.backboneClient = backboneClient;
    }

    /// Confirms the verification code for the specified user.
    ///
    /// @param sessionTokenBkd the backbone token session
    /// @param confirmCodeRequest the request object containing the user ID and verification code
    /// @return a ResponseEntity containing the response object and HTTP status
    @Override
    public ResponseEntity<Void> confirmCode(String sessionTokenBkd, ConfirmCodeRequest confirmCodeRequest) {
        var userResponse = backboneClient.findUserById(confirmCodeRequest.userId());
        AuthRequest mercuryAuthRequest = new AuthRequest(userResponse.alias(), userResponse.password());
        try {
            var mercuryToken = mercuryClient.token(sessionTokenBkd, mercuryAuthRequest);

            return mercuryClient.confirmCode(mercuryToken.token(),
                    confirmCodeMapper.toVerificationCodeRequest(confirmCodeRequest, applicationId));
        } catch (FeignException e) {
            Optional<String> message = e.responseHeaders().get(MESSAGE_HEADER).stream().findFirst();
            return ResponseEntity.status(e.status())
                    .header(MESSAGE_HEADER, message.orElse(""))
                    .build();
        }
    }
}
