package com.actionth.membership.controller;

import com.actionth.membership.model.request.TemplateEmailRequest;
import com.actionth.membership.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;


    @Operation(
        summary = "ส่งอีเมลโดยใช้เทมเพลต",
        description = "ส่งอีเมลโดยระบุชื่อเทมเพลตและพารามิเตอร์ต่างๆ ที่ต้องใช้ในเทมเพลตนั้น",
        responses = {
            @ApiResponse(responseCode = "200", description = "Email sent successfully"),
            @ApiResponse(responseCode = "400", description = "Bad Request - Missing required fields"),
            @ApiResponse(responseCode = "500", description = "Internal Server Error")
        }
    )
    @PostMapping("/send")
    public ResponseEntity<String> sendTemplateEmail(@RequestBody TemplateEmailRequest request) {
        if (request.getTo() == null || request.getSubject() == null || request.getTemplateName() == null) {
            return ResponseEntity.badRequest().body("Missing required fields: to, subject, and templateName");
        }

        emailService.sendGeneralTemplateEmail(request);
        return ResponseEntity.ok("Email queued.");
    }
}
