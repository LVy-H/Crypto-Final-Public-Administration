package com.gov.crypto.cloudsign.config;

import com.gov.crypto.common.tsa.TsaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudSignConfig {

    @Value("${tsa.url}")
    private String tsaUrl;

    @Value("${tsa.username}")
    private String tsaUsername;

    @Value("${tsa.password}")
    private String tsaPassword;

    @Value("${tsa.timeout:5000}")
    private int tsaTimeout;

    @Bean
    public TsaClient tsaClient() {
        return new TsaClient(tsaUrl, tsaUsername, tsaPassword, tsaTimeout);
    }
}
