package com.example.springboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private String sessionId = UUID.randomUUID().toString();
    private LinkedList<String> history = new LinkedList<>();

    @PostMapping
    public ResponseEntity<String> processMessage(@RequestBody String message) {
        // Process the message and return a response
        history.add(message);
        if (history.size() > 50) {
            history.removeFirst();
        }
        return ResponseEntity.ok("Processed message: " + message);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) throws IOException {
        // Handle file upload
        String fileName = file.getOriginalFilename();
        return ResponseEntity.ok("Received file: " + fileName);
    }
}
