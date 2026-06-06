package com.actionth.membership.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.model.dto.ShirtSizeDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.ShirtSizeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shirtSizes")
public class ShirtSizeController {

    private final ShirtSizeService shirtSizeService;

    @GetMapping("/getShirtSizeByType")
    public Response<List<ShirtSizeDto>> getShirtSizeByType(@RequestParam String id) {
        return new Response<>(shirtSizeService.getShirtSizeByShirtTypeId(id), "Shirt size retrieved successfully", true);
    }
}
