package com.example.showyourvoice.domain.place.controller;

import com.example.showyourvoice.domain.place.controller.dto.AssignmentRuleResponse;
import com.example.showyourvoice.domain.place.controller.dto.CreateAssignmentRuleRequest;
import com.example.showyourvoice.domain.place.controller.dto.UpdateAssignmentRuleRequest;
import com.example.showyourvoice.domain.place.service.AssignmentRuleService;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignment-rules")
@RequiredArgsConstructor
public class AssignmentRuleController {

    private final AssignmentRuleService service;

    @GetMapping
    public ResponseEntity<List<AssignmentRuleResponse>> getRules() {
        return ResponseEntity.ok(service.getRules());
    }

    @PostMapping
    public ResponseEntity<AssignmentRuleResponse> createRule(@Valid @RequestBody CreateAssignmentRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRule(request));
    }

    @PutMapping("/{roomId}/{category}")
    public ResponseEntity<AssignmentRuleResponse> updateRule(@PathVariable Long roomId,
                                                              @PathVariable PostCategory category,
                                                              @Valid @RequestBody UpdateAssignmentRuleRequest request) {
        return ResponseEntity.ok(service.updateRule(roomId, category, request));
    }

    @DeleteMapping("/{roomId}/{category}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long roomId, @PathVariable PostCategory category) {
        service.deleteRule(roomId, category);
        return ResponseEntity.noContent().build();
    }
}
