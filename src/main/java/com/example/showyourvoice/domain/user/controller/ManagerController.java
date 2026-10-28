package com.example.showyourvoice.domain.user.controller;

import com.example.showyourvoice.domain.user.controller.dto.ManagerInfoResponse;
import com.example.showyourvoice.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/managers")
@RequiredArgsConstructor
public class ManagerController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<ManagerInfoResponse>> getManagers() {
        return ResponseEntity.ok(userService.getManagers());
    }
}
