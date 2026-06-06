package com.actionth.membership.repository;

import com.actionth.membership.model.GeoProvince;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeoProvinceRepository extends JpaRepository<GeoProvince, Integer> {

    List<GeoProvince> findByActiveTrueOrderByNameEnAsc();

    List<GeoProvince> findByNameThAndActiveTrue(String nameTh);

    List<GeoProvince> findByNameEnAndActiveTrue(String nameEn);

    List<GeoProvince> findByNameThContainingAndActiveTrueOrderByNameEnAsc(String nameTh);

    List<GeoProvince> findByNameEnContainingIgnoreCaseAndActiveTrueOrderByNameEnAsc(String nameEn);
}
