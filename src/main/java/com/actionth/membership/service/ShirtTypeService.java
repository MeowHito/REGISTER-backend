package com.actionth.membership.service;

import com.actionth.membership.model.ShirtType;
import com.actionth.membership.repository.ShirtTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShirtTypeService {

    @Autowired
    private ShirtTypeRepository shirtTypeRepository;

    // ดึงข้อมูล shirttype ทั้งหมดที่ active = 1
    public List<ShirtType> getAllActiveShirtTypes() {
        return shirtTypeRepository.findAllActiveShirtTypes();
    }
}
