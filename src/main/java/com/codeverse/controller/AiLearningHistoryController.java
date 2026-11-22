
package com.codeverse.controller;

import com.codeverse.entity.AiLearningHistory;
import com.codeverse.entity.User;
import com.codeverse.service.AiLearningHistoryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/history")
public class AiLearningHistoryController {

    private final AiLearningHistoryService historyService;

    public AiLearningHistoryController(
            AiLearningHistoryService historyService
    ) {
        this.historyService = historyService;
    }

    // Get logged-in user's AI learning history
    @GetMapping
    public ResponseEntity<List<AiLearningHistory>> getHistory(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                historyService.getUserHistory(user.getId())
        );
    }

    // Get a specific saved explanation
    @GetMapping("/{id}")
    public ResponseEntity<AiLearningHistory> getHistoryById(
            @PathVariable String id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                historyService.getHistoryById(id, user.getId())
        );
    }

    // Delete a saved explanation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHistory(
            @PathVariable String id,
            @AuthenticationPrincipal User user
    ) {
        historyService.deleteHistory(id, user.getId());

        return ResponseEntity.noContent().build();
    }
}
