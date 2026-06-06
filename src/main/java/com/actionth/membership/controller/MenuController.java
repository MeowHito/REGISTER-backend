package com.actionth.membership.controller;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.MenuDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.MenuService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping
    public Response<MenuDto> createMenu(@RequestBody MenuDto dto) {
        return new Response<>(menuService.createMenu(dto), "Menu created successfully", true);
    }

    @PutMapping
    public Response<MenuDto> updateMenu(@RequestBody MenuDto dto) {
        return new Response<>(menuService.updateMenu(dto), "Menu updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteMenu(@PathVariable String uuid, @RequestParam("mode") String mode) {
        menuService.deleteMenu(uuid, mode);
        return new Response<>(null, "Menu deleted successfully", true);
    }

    @GetMapping("/{uuid}")
    public Response<MenuDto> getMenuByUuid(@PathVariable String uuid) {
        return new Response<>(menuService.getMenuByUuid(uuid), "Menu retrieved successfully", true);
    }

    @GetMapping
    public Response<Page<MenuDto>> getAllMenus(
            @RequestParam(value = "paging", required = false) String pagingJson,
            @RequestParam(value = "active", required = false) Boolean active) throws JsonProcessingException {

        PagingData paging = null;
        if (pagingJson != null) {
            paging = new ObjectMapper().readValue(pagingJson, PagingData.class);
        }

        return new Response<>(menuService.findAll(paging, active), "Menus retrieved successfully", true);
    }

    @PutMapping("/reorder")
    public ResponseEntity<Response<Void>> reorderMenus(@RequestBody List<MenuDto> menus) {
        menuService.reorderMenus(menus);
        return ResponseEntity.ok(new Response<>(null, "Reorder successful", true));
    }
}
