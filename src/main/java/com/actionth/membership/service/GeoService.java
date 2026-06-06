package com.actionth.membership.service;

import com.actionth.membership.model.GeoDistrict;
import com.actionth.membership.model.GeoProvince;
import com.actionth.membership.model.GeoSubdistrict;
import com.actionth.membership.model.dto.GeoDistrictDto;
import com.actionth.membership.model.dto.GeoLookupDto;
import com.actionth.membership.model.dto.GeoProvinceDto;
import com.actionth.membership.model.dto.GeoSubdistrictDto;
import com.actionth.membership.repository.GeoDistrictRepository;
import com.actionth.membership.repository.GeoProvinceRepository;
import com.actionth.membership.repository.GeoSubdistrictRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeoService {

    @Autowired
    private GeoProvinceRepository geoProvinceRepository;

    @Autowired
    private GeoDistrictRepository geoDistrictRepository;

    @Autowired
    private GeoSubdistrictRepository geoSubdistrictRepository;

    @Cacheable("geoProvinces")
    public List<GeoProvinceDto> getAllProvinces() {
        return geoProvinceRepository.findByActiveTrueOrderByNameEnAsc()
                .stream()
                .map(p -> GeoProvinceDto.builder()
                        .code(p.getCode())
                        .nameEn(p.getNameEn())
                        .nameTh(p.getNameTh())
                        .build())
                .collect(Collectors.toList());
    }

    public List<GeoDistrictDto> getDistrictsByProvinceCode(Integer provinceCode) {
        return geoDistrictRepository.findByProvinceCodeAndActiveTrueOrderByNameEnAsc(provinceCode)
                .stream()
                .map(this::toDistrictDto)
                .collect(Collectors.toList());
    }

    public List<GeoDistrictDto> getDistrictsByProvinceName(String provinceName) {
        List<GeoProvince> provinces = geoProvinceRepository.findByNameThAndActiveTrue(provinceName);
        if (provinces.isEmpty()) {
            provinces = geoProvinceRepository.findByNameEnAndActiveTrue(provinceName);
        }
        if (provinces.isEmpty()) {
            return new ArrayList<>();
        }
        return provinces.stream()
                .flatMap(p -> geoDistrictRepository.findByProvinceCodeAndActiveTrueOrderByNameEnAsc(p.getCode()).stream())
                .map(this::toDistrictDto)
                .collect(Collectors.toList());
    }

    public List<GeoSubdistrictDto> getSubdistrictsByDistrictCode(Integer districtCode) {
        return geoSubdistrictRepository.findByDistrictCodeAndActiveTrueOrderByNameEnAsc(districtCode)
                .stream()
                .map(this::toSubdistrictDto)
                .collect(Collectors.toList());
    }

    public List<GeoSubdistrictDto> getSubdistrictsByDistrictName(String districtName) {
        List<GeoDistrict> districts = geoDistrictRepository.findByNameThAndActiveTrue(districtName);
        if (districts.isEmpty()) {
            districts = geoDistrictRepository.findByNameEnIgnoreCaseAndActiveTrue(districtName);
        }
        if (districts.isEmpty()) {
            return new ArrayList<>();
        }
        return districts.stream()
                .flatMap(d -> geoSubdistrictRepository.findByDistrictCodeAndActiveTrueOrderByNameEnAsc(d.getCode()).stream())
                .map(this::toSubdistrictDto)
                .collect(Collectors.toList());
    }

    public List<GeoLookupDto> lookupByProvinceName(String provinceName) {
        List<GeoProvince> provinces = geoProvinceRepository.findByNameThAndActiveTrue(provinceName);
        if (provinces.isEmpty()) {
            provinces = geoProvinceRepository.findByNameEnAndActiveTrue(provinceName);
        }
        if (provinces.isEmpty()) {
            return new ArrayList<>();
        }

        List<GeoLookupDto> results = new ArrayList<>();
        for (GeoProvince province : provinces) {
            List<GeoDistrict> districts = geoDistrictRepository
                    .findByProvinceCodeAndActiveTrueOrderByNameEnAsc(province.getCode());
            for (GeoDistrict district : districts) {
                List<GeoSubdistrict> subdistricts = geoSubdistrictRepository
                        .findByDistrictCodeAndActiveTrueOrderByNameEnAsc(district.getCode());
                for (GeoSubdistrict s : subdistricts) {
                    results.add(GeoLookupDto.builder()
                            .postalCode(s.getPostalCode())
                            .provinceCode(province.getCode())
                            .provinceName(province.getNameTh())
                            .districtCode(district.getCode())
                            .districtName(district.getNameTh())
                            .subdistrictCode(s.getCode())
                            .subdistrictName(s.getNameTh())
                            .build());
                }
            }
        }
        return results;
    }

    public List<GeoLookupDto> lookupByDistrictName(String districtName) {
        List<GeoDistrict> districts = geoDistrictRepository.findByNameThAndActiveTrue(districtName);
        if (districts.isEmpty()) {
            districts = geoDistrictRepository.findByNameEnIgnoreCaseAndActiveTrue(districtName);
        }
        if (districts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> provinceCodes = districts.stream()
                .map(GeoDistrict::getProvinceCode)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, GeoProvince> provinceMap = geoProvinceRepository.findAllById(provinceCodes)
                .stream()
                .collect(Collectors.toMap(GeoProvince::getCode, p -> p));

        List<GeoLookupDto> results = new ArrayList<>();
        for (GeoDistrict district : districts) {
            GeoProvince province = provinceMap.get(district.getProvinceCode());
            List<GeoSubdistrict> subdistricts = geoSubdistrictRepository
                    .findByDistrictCodeAndActiveTrueOrderByNameEnAsc(district.getCode());
            for (GeoSubdistrict s : subdistricts) {
                results.add(GeoLookupDto.builder()
                        .postalCode(s.getPostalCode())
                        .provinceCode(province != null ? province.getCode() : null)
                        .provinceName(province != null ? province.getNameTh() : null)
                        .districtCode(district.getCode())
                        .districtName(district.getNameTh())
                        .subdistrictCode(s.getCode())
                        .subdistrictName(s.getNameTh())
                        .build());
            }
        }
        return results;
    }

    public List<GeoLookupDto> lookupBySubdistrictName(String subdistrictName) {
        List<GeoSubdistrict> subdistricts = geoSubdistrictRepository.findByNameThAndActiveTrue(subdistrictName);
        if (subdistricts.isEmpty()) {
            subdistricts = geoSubdistrictRepository.findByNameEnIgnoreCaseAndActiveTrue(subdistrictName);
        }
        if (subdistricts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> districtCodes = subdistricts.stream()
                .map(GeoSubdistrict::getDistrictCode)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, GeoDistrict> districtMap = geoDistrictRepository.findAllById(districtCodes)
                .stream()
                .collect(Collectors.toMap(GeoDistrict::getCode, d -> d));

        List<Integer> provinceCodes = districtMap.values().stream()
                .map(GeoDistrict::getProvinceCode)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, GeoProvince> provinceMap = geoProvinceRepository.findAllById(provinceCodes)
                .stream()
                .collect(Collectors.toMap(GeoProvince::getCode, p -> p));

        return subdistricts.stream()
                .map(s -> {
                    GeoDistrict district = districtMap.get(s.getDistrictCode());
                    GeoProvince province = district != null ? provinceMap.get(district.getProvinceCode()) : null;
                    return GeoLookupDto.builder()
                            .postalCode(s.getPostalCode())
                            .provinceCode(province != null ? province.getCode() : null)
                            .provinceName(province != null ? province.getNameTh() : null)
                            .districtCode(district != null ? district.getCode() : null)
                            .districtName(district != null ? district.getNameTh() : null)
                            .subdistrictCode(s.getCode())
                            .subdistrictName(s.getNameTh())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private GeoDistrictDto toDistrictDto(GeoDistrict d) {
        return GeoDistrictDto.builder()
                .code(d.getCode())
                .provinceCode(d.getProvinceCode())
                .nameEn(d.getNameEn())
                .nameTh(d.getNameTh())
                .build();
    }

    private GeoSubdistrictDto toSubdistrictDto(GeoSubdistrict s) {
        return GeoSubdistrictDto.builder()
                .code(s.getCode())
                .districtCode(s.getDistrictCode())
                .nameEn(s.getNameEn())
                .nameTh(s.getNameTh())
                .postalCode(s.getPostalCode())
                .build();
    }

    public List<GeoLookupDto> lookupByPostalCode(String postalCode) {
        List<GeoSubdistrict> subdistricts = geoSubdistrictRepository.findByPostalCodeAndActiveTrue(postalCode);

        if (subdistricts.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> districtCodes = subdistricts.stream()
                .map(GeoSubdistrict::getDistrictCode)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, GeoDistrict> districtMap = geoDistrictRepository.findAllById(districtCodes)
                .stream()
                .collect(Collectors.toMap(GeoDistrict::getCode, d -> d));

        List<Integer> provinceCodes = districtMap.values().stream()
                .map(GeoDistrict::getProvinceCode)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, GeoProvince> provinceMap = geoProvinceRepository.findAllById(provinceCodes)
                .stream()
                .collect(Collectors.toMap(GeoProvince::getCode, p -> p));

        return subdistricts.stream()
                .map(s -> {
                    GeoDistrict district = districtMap.get(s.getDistrictCode());
                    GeoProvince province = district != null ? provinceMap.get(district.getProvinceCode()) : null;

                    return GeoLookupDto.builder()
                            .postalCode(s.getPostalCode())
                            .provinceCode(province != null ? province.getCode() : null)
                            .provinceName(province != null ? province.getNameTh() : null)
                            .districtCode(district != null ? district.getCode() : null)
                            .districtName(district != null ? district.getNameTh() : null)
                            .subdistrictCode(s.getCode())
                            .subdistrictName(s.getNameTh())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
