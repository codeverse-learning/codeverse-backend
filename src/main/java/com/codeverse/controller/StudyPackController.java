package com.codeverse.controller;

import com.codeverse.dto.request.StudyPackRequest;
import com.codeverse.dto.request.StudyPackRenameRequest;
import com.codeverse.dto.response.StudyPackResponse;
import com.codeverse.entity.User;
import com.codeverse.service.StudyPackService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-packs")
@RequiredArgsConstructor
public class StudyPackController {

    private final StudyPackService studyPackService;

    // Generate Study Pack from pasted text or topic
    @PostMapping(
            value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StudyPackResponse> generateStudyPack(
            @Valid @RequestBody StudyPackRequest request,
            @AuthenticationPrincipal User user
    ) {

        StudyPackResponse response =
                studyPackService.generateFromContent(
                        getUserId(user),
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Generate Study Pack from PDF, DOCX or TXT file
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StudyPackResponse> uploadStudyPack(
            @RequestParam("file") MultipartFile file,

            @RequestParam(
                    value = "title",
                    required = false
            ) String title,

            @RequestParam(
                    value = "difficulty",
                    defaultValue = "Beginner"
            ) String difficulty,

            @RequestParam(
                    value = "language",
                    defaultValue = "English"
            ) String language,

            @AuthenticationPrincipal User user
    ) {

        StudyPackResponse response =
                studyPackService.generateFromFile(
                        getUserId(user),
                        file,
                        title,
                        difficulty,
                        language
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all Study Packs or search by title
    @GetMapping
    public ResponseEntity<List<StudyPackResponse>> getMyStudyPacks(
            @RequestParam(
                    value = "search",
                    required = false
            ) String search,

            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                studyPackService.getMyStudyPacks(
                        getUserId(user),
                        search
                )
        );
    }

    // Get total number of Study Packs
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countStudyPacks(
            @AuthenticationPrincipal User user
    ) {

        long count = studyPackService.countStudyPacks(
                getUserId(user)
        );

        return ResponseEntity.ok(
                Map.of("count", count)
        );
    }

    // Get a specific Study Pack
    @GetMapping("/{id}")
    public ResponseEntity<StudyPackResponse> getStudyPack(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                studyPackService.getStudyPack(
                        getUserId(user),
                        id
                )
        );
    }

    // Rename an existing Study Pack
    @PatchMapping("/{id}")
    public ResponseEntity<StudyPackResponse> renameStudyPack(
            @PathVariable("id") String id,

            @Valid @RequestBody
            StudyPackRenameRequest request,

            @AuthenticationPrincipal User user
    ) {

        return ResponseEntity.ok(
                studyPackService.renameStudyPack(
                        getUserId(user),
                        id,
                        request.getTitle()
                )
        );
    }

    // Delete a Study Pack
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudyPack(
            @PathVariable("id") String id,
            @AuthenticationPrincipal User user
    ) {

        studyPackService.deleteStudyPack(
                getUserId(user),
                id
        );

        return ResponseEntity.noContent().build();
    }

    // Retrieve authenticated user's ID
    private String getUserId(User user) {

        if (user == null
                || user.getId() == null
                || user.getId().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        return user.getId();
    }
}