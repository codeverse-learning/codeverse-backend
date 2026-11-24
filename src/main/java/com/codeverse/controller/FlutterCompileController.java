package com.codeverse.controller;

import com.codeverse.service.FlutterCompileService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/flutter")
public class FlutterCompileController {

    private final FlutterCompileService flutterCompileService;

    public FlutterCompileController(
            FlutterCompileService flutterCompileService
    ) {
        this.flutterCompileService = flutterCompileService;
    }

    @PostMapping("/compile")
    public ResponseEntity<Map<String, Object>> compile(
            @RequestBody Map<String, String> request
    ) {

        String code = request.get("code");

        Map<String, Object> result =
                flutterCompileService.compile(code);

        return ResponseEntity.ok(result);
    }
}