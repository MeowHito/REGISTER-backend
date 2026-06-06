package com.actionth.membership.service;

import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.dto.ShirtSizeDto;
import com.actionth.membership.repository.ShirtSizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ShirtSizeService {

    @Autowired
    private ShirtSizeRepository shirtSizeRepository;

    public List<ShirtSize> getAllShirtSizes() {
        return shirtSizeRepository.findAll();
    }

    public List<ShirtSizeDto> getShirtSizeByShirtTypeId(String shirtTypeId) {
        return shirtSizeRepository.findAllByShirtType_Uuid(shirtTypeId).stream()
                .map(shirtSize -> {
                    ShirtSizeDto dto = new ShirtSizeDto();
                    dto.setId(shirtSize.getUuid());
                    dto.setName(shirtSize.getName());
                    dto.setChestSize(shirtSize.getChestSize());
                    dto.setLengthSize(shirtSize.getLengthSize());
                    return dto;
                })
                .toList();
    }

}
