package com.example.attendance.controller;

import com.example.attendance.model.AttendanceRecord;
import com.example.attendance.model.Student;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.StudentRepository;
import com.example.attendance.service.FaceRecognitionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")

public class AttendanceController {
    private final FaceRecognitionService faceRecognitionService;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;

    public AttendanceController(FaceRecognitionService faceRecognitionService,
                                StudentRepository studentRepository,
                                AttendanceRepository attendanceRepository) {
        this.faceRecognitionService = faceRecognitionService;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @PostMapping("/attendance/scan")
    public ResponseEntity<?> scanAttendance(@RequestBody Map<String, String> body) {
        String base64 = body.get("imageBase64");
        if (base64 == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing imageBase64"));
        }

        // Remove data URI prefix if present
        if (base64.startsWith("data:image")) {
            int commaIndex = base64.indexOf(',');
            if (commaIndex > 0) {
                base64 = base64.substring(commaIndex + 1);
            }
        }

        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
        Long studentId = faceRecognitionService.identifyStudent(imageBytes);
        if (studentId == null) {
            return ResponseEntity.ok(Map.of("recognized", false, "message", "Face not recognized"));
        }

        Optional<Student> student = studentRepository.findById(studentId);
        if (student.isEmpty()) {
            return ResponseEntity.ok(Map.of("recognized", false, "message", "Student not found"));
        }

        AttendanceRecord record = new AttendanceRecord(studentId, student.get().getUsername(), LocalDateTime.now());
        attendanceRepository.save(record);

        return ResponseEntity.ok(Map.of(
                "recognized", true,
                "studentId", studentId,
                "username", student.get().getUsername(),
                "timestamp", record.getTimestamp().toString()
        ));
    }

    @GetMapping("/attendance")
    public List<AttendanceRecord> getAttendance() {
        return attendanceRepository.findTop100ByOrderByTimestampDesc();
    }

    @GetMapping("/students")
    public List<Student> getStudents() {
        return studentRepository.findAll();
    }
}