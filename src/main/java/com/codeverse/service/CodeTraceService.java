
package com.codeverse.service;

import com.codeverse.dto.request.CodeTraceRequest;
import com.codeverse.dto.response.CodeTraceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Locale;

@Service
public class CodeTraceService {

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

        if (!language.equals("python")) {
            return new CodeTraceResponse(
                    language,
                    false,
                    List.of(),
                    "",
                    "Only Python is supported in Phase 1."
            );
        }

        try {
            CodeTraceResponse response = restClient.post()
                    .uri("/trace")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(
                            new com.fasterxml.jackson.databind.ObjectMapper()
                                    .writeValueAsString(request)
                    )
                    .retrieve()
                    .body(CodeTraceResponse.class);

            if (response == null) {
                throw new IllegalStateException(
                        "Empty response from Python gateway"
                );
            }

            return response;

        } catch (Exception exception) {

            exception.printStackTrace();

            return new CodeTraceResponse(
                    language,
                    false,
                    List.of(),
                    "",
                    "Python execution service is unavailable."
            );
        }
    }
}
