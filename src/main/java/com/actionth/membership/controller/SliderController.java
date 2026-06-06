package com.actionth.membership.controller;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.SliderDTO;
import com.actionth.membership.model.request.SliderReorderDTO;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.SliderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/slider")
public class SliderController {

    @Autowired
    private SliderService sliderService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping
    public Response<Page<SliderDTO>> getSlidersWithPagination(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(sliderService.findAll(paging), "Sliders retrieved successfully", true);
    }

    @GetMapping("/{id}")
    public Response<SliderDTO> getSliderById(@PathVariable String id) {
        return new Response<>(sliderService.findByUuid(id), "Slider retrieved successfully", true);
    }

    @PostMapping
    public Response<Void> createSlider(@Valid @RequestBody SliderDTO slider) {
        try {
            sliderService.createSlider(slider);
            return new Response<>(null, "Slider created successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to create slider", false);
        }
    }

    @PutMapping
    public Response<Void> updateSlider(@Valid @RequestBody SliderDTO slider) {
        try {
            sliderService.updateSlider(slider);
            return new Response<>(null, "Slider updated successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to update slider", false);
        }
    }

    @DeleteMapping("/{id}")
    public Response<Void> deleteSlider(
            @PathVariable String id,
            @RequestParam(value = "mode", required = true) String mode) {
        try {
            sliderService.deleteSlider(id, mode);
            return new Response<>(null, "Slider deleted successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to delete slider", false);
        }
    }

    @PutMapping("/reorder")
    public Response<Void> reorderSliders(@Valid @RequestBody SliderReorderDTO reorderDTO) {
        try {
            sliderService.reorderSliders(reorderDTO);
            return new Response<>(null, "Slider order updated successfully", true);
        } catch (BusinessException e) {
            return new Response<>(null, e.getMessage(), false);
        } catch (Exception e) {
            return new Response<>(null, "Failed to update slider order", false);
        }
    }
}
