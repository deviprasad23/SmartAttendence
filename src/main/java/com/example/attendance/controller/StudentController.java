package com.example.attendance.controller;

import com.example.attendance.model.Student;
import com.example.attendance.repository.StudentRepository;
import com.example.attendance.service.FaceRecognitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")

public class StudentController {

    private final StudentRepository studentRepository;
    private final FaceRecognitionService faceRecognitionService;

    public StudentController(StudentRepository studentRepository, FaceRecognitionService faceRecognitionService) {
        this.studentRepository = studentRepository;
        this.faceRecognitionService = faceRecognitionService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");

        if (username == null || username.isBlank() || email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Missing required fields");
        }

        if (studentRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        Student student = new Student(username, email, password);
        studentRepository.save(student);

        Map<String, Object> rsp = new HashMap<>();
        rsp.put("message", "Student registered successfully");
        rsp.put("studentId", student.getId());
        return ResponseEntity.ok(rsp);
    }

    @PostMapping(path = "/students/{studentId}/faces", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadFaces(@PathVariable Long studentId, @RequestParam("images") MultipartFile[] images) {
        if (!studentRepository.existsById(studentId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            int index = 0;
            for (MultipartFile file : images) {
                if (file.isEmpty()) continue;
                faceRecognitionService.saveFaceImage(studentId, index++, file.getBytes());
            }
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Failed to store images");
        }

        return ResponseEntity.ok(Map.of("message", "Face images uploaded"));
    }
}