package com.prx.directory.client.interceptor;

import com.prx.commons.general.pojo.UserSession;
import com.prx.commons.general.to.TokenResponse;
import com.prx.security.properties.ClientProperties;
import feign.RequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.springframework.cloud.openfeign.security.OAuth2AccessTokenInterceptor.BEARER;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

public abstract class BackendFeignConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackendFeignConfigurer.class);
    protected static final String BACKBONE_ID = "backbone";
    protected static final String MERCURY_ID = "mercury";

    @Value("${prx.logging.trace.enabled}")
    private boolean isTraceEnabled;

    private final ClientProperties clientProperties;

    protected BackendFeignConfigurer(ClientProperties clientProperties) {
        this.clientProperties = clientProperties;
    }

    abstract RequestInterceptor requestInterceptor();

    protected RequestInterceptor createInterceptor() {
        return template -> {
            try {
                assert clientProperties != null;
                String token = getToken(clientProperties);
                template.header(AUTHORIZATION, BEARER.concat(" ").concat(token));
                if (isTraceEnabled) {
                    logHeaders(template.headers());
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    protected String getToken(ClientProperties clientProperties) throws Exception {
        RestTemplate client = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", clientProperties.getAuthorizationGrantType());
        parameters.add("client_id", clientProperties.getClientId());
        parameters.add("username", clientProperties.getUsername());
        parameters.add("password", clientProperties.getPassword());
        parameters.add("client_secret", clientProperties.getClientSecret());

        var response = client.postForObject(clientProperties.getRedirectUri(), new HttpEntity<>(parameters, headers), TokenResponse.class);
        if (Objects.isNull(response)) {
            LOGGER.error("Error occurred while connecting with the Manager authenticator");
            throw new Exception("Error occurred while connecting with the Manager authenticator");
        }
        return create(response, UUID.randomUUID()).token();
    }

    private void logHeaders(Map<String, Collection<String>> headers) {
        LOGGER.info("Headers key/value :::::");
        headers.forEach((key, value) -> LOGGER.info("KEY: {}, VALUE: {} :::::", key, value));
    }

    protected static UserSession create(TokenResponse tokenResponse, UUID id) {
        return new UserSession(id, null, tokenResponse.accessToken());
    }

    public final String getClientId() {
        return clientProperties.getClientId();
    }
    
}
