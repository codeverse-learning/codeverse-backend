
package com.codeverse.controller;

import com.codeverse.dto.request.CodeTraceRequest;
import com.codeverse.dto.response.CodeTraceResponse;
import com.codeverse.service.CodeTraceService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/code-trace")
@RequiredArgsConstructor
public class CodeTraceController {

    private final CodeTraceService codeTraceService;

    @PostMapping
    public ResponseEntity<CodeTraceResponse> traceCode(
            @Valid @RequestBody CodeTraceRequest request
    ) {

        CodeTraceResponse response =
                codeTraceService.traceCode(request);

        return ResponseEntity.ok(response);
    }
}
