package com.prx.directory.client.interceptor;

import com.prx.commons.general.pojo.UserSession;
import com.prx.commons.general.to.TokenResponse;
import com.prx.security.properties.AuthProperties;
import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.UUID;

import static com.prx.commons.constants.keys.ManagementAuthKey.*;
import static org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor.BEARER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

///  The BackendFeignClientInterceptor class is responsible for intercepting requests to the backend service.
@Configuration
public class BackendFeignClientInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendFeignClientInterceptor.class);

    @Value("${prx.logging.trace.enabled}")
    private boolean isTraceEnabled;

    private final AuthProperties authProperties;

    ///  Constructor for BackendFeignClientInterceptor.
    ///
    /// @param authProperties the properties for authenticating with the backend service
    public BackendFeignClientInterceptor(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    ///  Creates a request interceptor for adding the session token to the request.
    /// The session token is retrieved by making a request to the Manager authenticator.
    ///
    /// @return the request interceptor
    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String token;
            try {
                token = getToken();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Add the session-token header to the request
            template.header(AUTHORIZATION, BEARER.concat(" ").concat(token));
            if (isTraceEnabled) {
                LOGGER.info("Headers key/value :::::");
                template.headers().forEach((key, value) ->
                        LOGGER.info("KEY: {}, VALUE: {} :::::", key, value));
            }
        };
    }

    private String getToken() throws Exception {
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        parameters.add(GRANT_TYPE.value, authProperties.getAuthorizationGrantType());
        parameters.add(CLIENT_ID.value, authProperties.getClientId());
        parameters.add(USERNAME.value, authProperties.getUsername());
        parameters.add(PASSWORD.value, authProperties.getPassword());
        parameters.add(CLIENT_SECRET.value, authProperties.getClientSecret());

        var response = client.postForObject(authProperties.getRedirectUri(), new HttpEntity<>(parameters, headers), TokenResponse.class);
        if (Objects.isNull(response)) {
            LOGGER.error("Error occurred while connect with the Manager authenticator");
            throw new Exception("Error occurred while connect with the Manager authenticator");
        }
        return create(response, UUID.randomUUID()).token();
    }

    public static UserSession create(TokenResponse tokenResponse, UUID id) {
        return new UserSession(id, null, tokenResponse.accessToken());
    }

}
