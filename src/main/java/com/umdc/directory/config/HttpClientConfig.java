package com.umdc.directory.config;

import com.umdc.security.exception.CertificateSecurityException;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

@Configuration
public class HttpClientConfig {

    private final String trustStoreLocation;
    private final String trustStorePassword;

    public HttpClientConfig(
            @Value("${umdc.security.truststore.location:prx-truststore.jks}") String trustStoreLocation,
            @Value("${umdc.security.truststore.password:changeit}") String trustStorePassword) {
        this.trustStoreLocation = trustStoreLocation;
        this.trustStorePassword = trustStorePassword;
    }

    @Bean
    @Primary
    public RestTemplate interServiceRestTemplate() throws CertificateSecurityException {
        try {
            // "classpath:name", "file:/path" or a bare name (classpath), the same locations the TLS bundle accepts
            Resource trustStoreResource = new DefaultResourceLoader().getResource(trustStoreLocation);
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(trustStoreResource.getURL(), trustStorePassword.toCharArray())
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(
                            PoolingHttpClientConnectionManagerBuilder.create()
                                    .setSSLSocketFactory(sslSocketFactory)
                                    .build())
                    .build();

            return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
        } catch (Exception e) {
            throw new CertificateSecurityException(
                    "Failed to initialise inter-service RestTemplate SSL context", e);
        }
    }
}
