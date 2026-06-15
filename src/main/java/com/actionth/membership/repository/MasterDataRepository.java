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
        // NOTE: the `countryState` table has no `active` column (see schema-prod.sql and
        // the CountryState entity). Selecting/filtering on `active` throws a SQL error and
        // makes the province dropdown show "no data". Query only the columns that exist.
        String sql = "SELECT uuid as id, countryEn, countryLocal, stateEn, stateLocal, stateType FROM countryState";
        return jdbcTemplate.queryForList(sql);
    }
    
    public List<Map<String, Object>> findAllNationalities() {
        String sql = "SELECT alpha_3_code, nationality FROM countries";
        return jdbcTemplate.queryForList(sql);
    }
    
}
