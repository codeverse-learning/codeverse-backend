package com.codeverse.service;

import com.codeverse.dto.request.CodeTraceRequest;
import com.codeverse.dto.response.CodeTraceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CodeTraceService {

    // Supported programming languages
    private static final Set<String> ENABLED_LANGUAGES = Set.of(
            "python",
            "javascript",
            "java",
            "c",
            "cpp",
            "csharp",
            "php",
            "dart"
    );

    private final RestClient restClient;

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    // Code Time Machine feature flag
    private final boolean traceEnabled;

    public CodeTraceService(

            @Value("${codeverse.trace.gateway-url:http://127.0.0.1:8091}")
            String gatewayUrl,

            @Value("${codeverse.worker.key}")
            String workerKey,

            @Value("${codeverse.trace.enabled:false}")
            boolean traceEnabled
    ) {

        this.traceEnabled = traceEnabled;

        // Do not initialize the worker client when disabled
        if (!traceEnabled) {

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
                Duration.ofSeconds(120)
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

    public CodeTraceResponse traceCode(
            CodeTraceRequest request
    ) {

        // Validate programming language
        if (request == null
                || request.language() == null
                || request.language().isBlank()) {

            return failure(
                    "unknown",
                    "Programming language is required."
            );
        }

        String language = request.language()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Block code execution when the feature is disabled
        if (!traceEnabled || restClient == null) {

            return failure(
                    language,
                    "Code Time Machine is currently unavailable."
            );
        }

        // Validate supported programming language
        if (!ENABLED_LANGUAGES.contains(language)) {

            return failure(
                    language,
                    "Unsupported programming language."
            );
        }

        // Validate source code
        if (request.code() == null
                || request.code().isBlank()) {

            return failure(
                    language,
                    "Code is required."
            );
        }

        // Validate maximum code length
        if (request.code().length() > 10000) {

            return failure(
                    language,
                    "Code exceeds the maximum allowed length."
            );
        }

        try {

            byte[] requestBody =
                    objectMapper.writeValueAsBytes(request);

            CodeTraceResponse response = restClient.post()
                    .uri("/trace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(requestBody.length)
                    .body(requestBody)
                    .retrieve()
                    .body(CodeTraceResponse.class);

            if (response == null) {

                throw new IllegalStateException(
                        "Empty response from code tracing gateway"
                );
            }

            return response;

        } catch (Exception exception) {

            System.err.println(
                    "Code trace gateway failure: "
                            + exception.getClass().getSimpleName()
            );

            return failure(
                    language,
                    "Code execution service is unavailable."
            );
        }
    }

    private CodeTraceResponse failure(
            String language,
            String message
    ) {

        return new CodeTraceResponse(
                language,
                false,
                List.of(),
                "",
                message
        );
    }
}