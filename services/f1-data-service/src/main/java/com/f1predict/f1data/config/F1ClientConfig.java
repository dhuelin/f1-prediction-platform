package com.f1predict.f1data.config;

import com.f1predict.f1data.client.JolpicaClient;
import com.f1predict.f1data.client.OpenF1Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class F1ClientConfig {

    @Bean
    public JolpicaClient jolpicaClient(@Value("${f1.jolpica.base-url}") String baseUrl) {
        return new JolpicaClient(baseUrl);
    }

    @Bean
    public OpenF1Client openF1Client(@Value("${f1.openf1.base-url}") String baseUrl) {
        return new OpenF1Client(baseUrl);
    }
}
