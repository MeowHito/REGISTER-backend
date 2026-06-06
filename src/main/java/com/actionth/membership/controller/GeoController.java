package com.actionth.membership.controller;

import com.actionth.membership.model.dto.GeoDistrictDto;
import com.actionth.membership.model.dto.GeoLookupDto;
import com.actionth.membership.model.dto.GeoProvinceDto;
import com.actionth.membership.model.dto.GeoSubdistrictDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.GeoService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public-api/master/geo")
public class GeoController {

    @Autowired
    private GeoService geoService;

    @GetMapping("/provinces")
    public Response<List<GeoProvinceDto>> getAllProvinces() {
        List<GeoProvinceDto> provinces = geoService.getAllProvinces();
        return new Response<>(provinces, "Provinces retrieved successfully", true);
    }

    @GetMapping("/districts")
    public Response<List<GeoDistrictDto>> getDistricts(
            @RequestParam(required = false) Integer provinceCode,
            @RequestParam(required = false) String provinceName) {
        List<GeoDistrictDto> districts;
        if (provinceCode != null) {
            districts = geoService.getDistrictsByProvinceCode(provinceCode);
        } else if (provinceName != null) {
            districts = geoService.getDistrictsByProvinceName(provinceName);
        } else {
            return new Response<>(List.of(), "provinceCode or provinceName is required", false);
        }
        return new Response<>(districts, "Districts retrieved successfully", true);
    }

    @GetMapping("/subdistricts")
    public Response<List<GeoSubdistrictDto>> getSubdistricts(
            @RequestParam(required = false) Integer districtCode,
            @RequestParam(required = false) String districtName) {
        List<GeoSubdistrictDto> subdistricts;
        if (districtCode != null) {
            subdistricts = geoService.getSubdistrictsByDistrictCode(districtCode);
        } else if (districtName != null) {
            subdistricts = geoService.getSubdistrictsByDistrictName(districtName);
        } else {
            return new Response<>(List.of(), "districtCode or districtName is required", false);
        }
        return new Response<>(subdistricts, "Subdistricts retrieved successfully", true);
    }

    @GetMapping("/lookup")
    public Response<List<GeoLookupDto>> lookup(
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String provinceName,
            @RequestParam(required = false) String districtName,
            @RequestParam(required = false) String subdistrictName) {
        List<GeoLookupDto> results;
        if (postalCode != null) {
            results = geoService.lookupByPostalCode(postalCode);
        } else if (subdistrictName != null) {
            results = geoService.lookupBySubdistrictName(subdistrictName);
        } else if (districtName != null) {
            results = geoService.lookupByDistrictName(districtName);
        } else if (provinceName != null) {
            results = geoService.lookupByProvinceName(provinceName);
        } else {
            return new Response<>(List.of(), "At least one search parameter is required", false);
        }
        return new Response<>(results, "Lookup completed successfully", true);
    }
}
