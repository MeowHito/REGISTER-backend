package com.actionth.membership.repository;

import com.actionth.membership.model.GeoSubdistrict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoSubdistrictRepository extends JpaRepository<GeoSubdistrict, Integer> {

    List<GeoSubdistrict> findByDistrictCodeAndActiveTrueOrderByNameEnAsc(Integer districtCode);

    List<GeoSubdistrict> findByPostalCodeAndActiveTrue(String postalCode);

    List<GeoSubdistrict> findByNameThAndActiveTrue(String nameTh);

    List<GeoSubdistrict> findByNameEnIgnoreCaseAndActiveTrue(String nameEn);
}
