package com.actionth.membership.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MasterDataRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getAllProvince() {
        String sql = "SELECT id, province, amphoe, district, zipcode, active FROM province WHERE active = true";
        return jdbcTemplate.queryForList(sql);
    }
    
    public List<Map<String, Object>> getAllCountryState() {
        String sql = "SELECT active, uuid as id, countryEn, countryLocal, stateEn, stateLocal, stateType FROM countryState WHERE active = true";
        return jdbcTemplate.queryForList(sql);
    }
    
    public List<Map<String, Object>> findAllNationalities() {
        String sql = "SELECT alpha_3_code, nationality FROM countries";
        return jdbcTemplate.queryForList(sql);
    }
    
}
