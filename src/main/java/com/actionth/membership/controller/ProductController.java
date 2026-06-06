package com.actionth.membership.controller;

import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.service.ShirtSizeService;
import com.actionth.membership.service.ShirtTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ShirtTypeService shirtTypeService;

    @Autowired  
    private ShirtSizeService shirtSizeService;

    // API `/api/product/shirt-types`
    @Operation(summary = "Get all active shirt types", description = "ดึงข้อมูลเสื้อทั้งหมดที่ active = 1")
    @ApiResponse(responseCode = "200", description = "พบข้อมูลเสื้อ",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ShirtType.class)))
    @GetMapping("/shirt-types")
    public ResponseEntity<List<ShirtType>> getAllActiveShirtTypes() {
        List<ShirtType> shirtTypes = shirtTypeService.getAllActiveShirtTypes();
        return ResponseEntity.ok(shirtTypes);
    }
    // API `/api/product/shirt-size`
    @GetMapping("/shirt-size")
    public ResponseEntity<List<ShirtSize>> getAllShirtSizes() {
        List<ShirtSize> shirtSizes = shirtSizeService.getAllShirtSizes();
        return ResponseEntity.ok(shirtSizes);
    }
}
