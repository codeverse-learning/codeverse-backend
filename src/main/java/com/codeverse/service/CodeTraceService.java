
package com.codeverse.service;

import com.codeverse.dto.request.CodeTraceRequest;
import com.codeverse.dto.response.CodeTraceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CodeTraceService {

    // Languages with working Docker execution support
    private static final Set<String> ENABLED_LANGUAGES = Set.of(
            "python",
            "javascript",
            "java"
    );

    // All planned CODEVERSE languages
    private static final Set<String> PLANNED_LANGUAGES = Set.of(
            "python",
            "javascript",
            "java",
            "c",
            "cpp",
            "csharp",
            "php",
            "go",
            "kotlin",
            "dart",
            "r",
            "bash"
    );

    private final RestClient restClient;

    public CodeTraceService(
            @Value("${codeverse.trace.gateway-url:http://127.0.0.1:8091}")
            String gatewayUrl
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }

    public CodeTraceResponse traceCode(CodeTraceRequest request) {

        String language = request.language()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Validate programming language
        if (!PLANNED_LANGUAGES.contains(language)) {
            return failure(
                    language,
                    "Unsupported programming language."
            );
        }

        // Check whether execution is implemented
        if (!ENABLED_LANGUAGES.contains(language)) {
            return failure(
                    language,
                    "Execution support for " + language
                            + " is coming soon."
            );
        }

        try {
            // Send language and code to multi-language gateway
            String requestJson =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(request);

            CodeTraceResponse response = restClient.post()
                    .uri("/trace")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
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
                            + exception.getMessage()
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
