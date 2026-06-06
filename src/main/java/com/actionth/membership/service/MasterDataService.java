package com.actionth.membership.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.actionth.membership.repository.MasterDataRepository;

@Service
public class MasterDataService {
    
    @Autowired
    private MasterDataRepository masterDataRepository;

    @Cacheable("provinces")
    public List<Map<String, Object>> getAllProvince() {
        return masterDataRepository.getAllProvince();
    }
    
    public List<Map<String, Object>> getAllCountryState() {
        return masterDataRepository.getAllCountryState();
    }

    public List<Map<String, Object>> getAllNationalities() {
        return masterDataRepository.findAllNationalities();
    }
}
