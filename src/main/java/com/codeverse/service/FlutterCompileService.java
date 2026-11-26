package com.codeverse.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.core.ParameterizedTypeReference;

import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class FlutterCompileService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // Flutter compiler feature flag
    private final boolean flutterEnabled;

    public FlutterCompileService(

            @Value("${codeverse.flutter.gateway-url:http://127.0.0.1:8092}")
            String gatewayUrl,

            @Value("${codeverse.worker.key}")
            String workerKey,

            @Value("${codeverse.flutter.enabled:false}")
            boolean flutterEnabled
    ) {

        this.flutterEnabled = flutterEnabled;
        this.objectMapper = new ObjectMapper();

        // Do not initialize the worker client when disabled
        if (!flutterEnabled) {

            this.restClient = null;
            return;
        }

        // Validate worker security key
        if (workerKey == null || workerKey.length() < 32) {

            throw new IllegalStateException(
                    "Worker security key is missing or invalid."
            );
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                Duration.ofSeconds(210)
        );

        this.restClient = RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestFactory(requestFactory)
                .defaultHeader(
                        "X-Worker-Key",
                        workerKey
                )
                .build();
    }

    public Map<String, Object> compile(String code) {

        // Block compilation when the feature is disabled
        if (!flutterEnabled || restClient == null) {

            return Map.of(
                    "success", false,
                    "error",
                    "Flutter compilation is currently unavailable."
            );
        }

        // Validate Flutter code
        if (code == null || code.isBlank()) {

            return Map.of(
                    "success", false,
                    "error", "Flutter code is required."
            );
        }

        // Validate maximum code length
        if (code.length() > 30000) {

            return Map.of(
                    "success", false,
                    "error",
                    "Code exceeds the maximum allowed length."
            );
        }

        try {

            byte[] requestBody =
                    objectMapper.writeValueAsBytes(
                            Map.of("code", code)
                    );

            Map<String, Object> response = restClient.post()
                    .uri("/compile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(requestBody.length)
                    .body(requestBody)
                    .retrieve()
                    .body(
                            new ParameterizedTypeReference
                                    <Map<String, Object>>() {}
                    );

            if (response == null) {

                return Map.of(
                        "success", false,
                        "error",
                        "Empty response from Flutter compiler."
                );
            }

            return response;

        } catch (Exception e) {

            System.err.println(
                    "Flutter compiler error: "
                            + e.getClass().getSimpleName()
            );

            return Map.of(
                    "success", false,
                    "error",
                    "Flutter compiler is unavailable or timed out."
            );
        }
    }
}