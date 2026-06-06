package com.actionth.membership.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.response.Response;
import com.actionth.membership.service.MasterDataService;

@RestController
@RequestMapping("/public-api/master")
public class MasterDataController {

    @Autowired
    private MasterDataService masterDataService;

    @GetMapping("/provinces")
    public Response<List<Map<String, Object>>> getAllProvinces() {
        List<Map<String, Object>> provinces = masterDataService.getAllProvince();
        return new Response<>(provinces, "Provinces retrieved successfully", true);
    }

    @GetMapping("/countryState")
    public Response<List<Map<String, Object>>> getAllCountryState() {
        List<Map<String, Object>> provinces = masterDataService.getAllCountryState();
        return new Response<>(provinces, "Country state retrieved successfully", true);
    }

    @GetMapping("/nationalities")
    public Response<List<Map<String, Object>>> getNationalities() {
        List<Map<String, Object>> nationalities = masterDataService.getAllNationalities();
        return new Response<>(nationalities, "Nationalities retrieved successfully", true);
    }
}
