package com.email.writer.controller;

import com.email.writer.domain.EmailRequest;
import com.email.writer.service.EmailService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/email")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/generate-email")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest){
        return ResponseEntity.ok(emailService.generateEmailReply(emailRequest));
    }
    private final Tika tika = new Tika();

    @PostMapping("/summary")
    public ResponseEntity<String> extractContent(@RequestParam("file") MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            String fileType = tika.detect(file.getBytes());
            String extractedContent = emailService.generateSummary(inputStream, fileType);
            return ResponseEntity.ok(extractedContent);
        } catch (IOException | TikaException | SAXException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}
