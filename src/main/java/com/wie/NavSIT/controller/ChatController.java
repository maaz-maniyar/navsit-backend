package com.wie.NavSIT.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // allow React frontend
public class ChatController {

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> handleQuery(@RequestBody Map<String, String> payload) {
        String userMessage = payload.get("message");

        // For now, return a sample response
        Map<String, Object> response = new HashMap<>();
        response.put("reply", "Got your message: " + userMessage);
        response.put("intent", "navigate");
        response.put("entity", "ECE Block");
        response.put("path", new String[]{"Main Gate", "Library", "ECE Block"});

        return ResponseEntity.ok(response);
    }
}
