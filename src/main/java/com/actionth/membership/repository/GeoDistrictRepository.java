package com.actionth.membership.repository;

import com.actionth.membership.model.GeoDistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoDistrictRepository extends JpaRepository<GeoDistrict, Integer> {

    List<GeoDistrict> findByProvinceCodeAndActiveTrueOrderByNameEnAsc(Integer provinceCode);

    List<GeoDistrict> findByNameThAndActiveTrue(String nameTh);

    List<GeoDistrict> findByNameEnIgnoreCaseAndActiveTrue(String nameEn);
}
