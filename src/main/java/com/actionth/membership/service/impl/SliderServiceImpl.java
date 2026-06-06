package com.actionth.membership.service.impl;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.Slider;
import com.actionth.membership.model.request.SliderDTO;
import com.actionth.membership.model.request.SliderReorderDTO;
import com.actionth.membership.repository.SliderRepository;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.SliderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SliderServiceImpl implements SliderService {

    private final SliderRepository sliderRepository;
    private final AWSService awsService;

    @Override
    public List<SliderDTO> findAllActive() {
        List<Slider> sliders = sliderRepository.findAllActiveOrderByPosition();
        return sliders.stream()
                .map(this::convertToDTO)
                .toList();
    }

    @Override
    public Page<SliderDTO> findAll(PagingData pagingData) {
        Sort sort = Sort.by(Sort.Direction.ASC, "position");
        if (pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<Slider> spec = (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return null;
        };

        if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                    .like(root.get(pagingData.getSearchField()), "%" + pagingData.getSearchText() + "%"));
        }
        Page<Slider> sliders = sliderRepository.findAll(spec, pageable);

        return sliders.map(this::convertToDTO);
    }

    @Override
    public SliderDTO findByUuid(String uuid) {
        Optional<Slider> slider = sliderRepository.findByUuid(uuid);
        if (!slider.isPresent()) {
            throw new ResourceNotFoundException("Slider not found with id: " + uuid);
        }
        return convertToDTO(slider.get());
    }

    @Override
    public void createSlider(SliderDTO sliderDTO) {
        validateSliderDTO(sliderDTO);

        Slider slider = new Slider();
        slider.setDescriptionEn(sliderDTO.getDescriptionEn());
        slider.setDescriptionTh(sliderDTO.getDescriptionTh());
        slider.setImageUrl(sliderDTO.getImageUrl());
        slider.setAlignment(sliderDTO.getAlignment() != null ? sliderDTO.getAlignment() : "text-center");
        slider.setPosition(sliderDTO.getPosition());
        slider.setActive(sliderDTO.getActive() != null ? sliderDTO.getActive() : true);

        sliderRepository.save(slider);
    }

    @Override
    public void updateSlider(SliderDTO sliderDTO) {
        if (sliderDTO.getId() == null) {
            throw new BusinessException("Slider ID is required for update");
        }

        validateSliderDTO(sliderDTO);

        Optional<Slider> existingSliderOpt = sliderRepository.findByUuid(sliderDTO.getId());
        if (!existingSliderOpt.isPresent()) {
            throw new ResourceNotFoundException("Slider not found with id: " + sliderDTO.getId());
        }

        Slider slider = existingSliderOpt.get();
        slider.setDescriptionEn(sliderDTO.getDescriptionEn());
        slider.setDescriptionTh(sliderDTO.getDescriptionTh());
        slider.setImageUrl(sliderDTO.getImageUrl());
        slider.setAlignment(sliderDTO.getAlignment() != null ? sliderDTO.getAlignment() : "text-center");
        slider.setPosition(sliderDTO.getPosition());
        slider.setActive(sliderDTO.getActive() != null ? sliderDTO.getActive() : true);

        sliderRepository.save(slider);
    }

    @Override
    public void deleteSlider(String uuid, String mode) {
        if (!"hard".equalsIgnoreCase(mode)) {
            throw new BusinessException("Only hard delete is supported. Please provide mode=hard");
        }

        Optional<Slider> slider = sliderRepository.findByUuid(uuid);
        if (!slider.isPresent()) {
            throw new ResourceNotFoundException("Slider not found with id: " + uuid);
        }

        sliderRepository.deleteByUuid(uuid);
    }

    @Override
    public void reorderSliders(SliderReorderDTO reorderDTO) {
        if (reorderDTO.getSliders() == null || reorderDTO.getSliders().isEmpty()) {
            throw new BusinessException("Sliders list cannot be empty");
        }

        for (SliderReorderDTO.SliderPositionDTO sliderPosition : reorderDTO.getSliders()) {
            Optional<Slider> sliderOpt = sliderRepository.findByUuid(sliderPosition.getId());
            if (sliderOpt.isPresent()) {
                Slider slider = sliderOpt.get();
                slider.setPosition(sliderPosition.getPosition());
                sliderRepository.save(slider);
            }
        }
    }

    private void validateSliderDTO(SliderDTO sliderDTO) {
        if (sliderDTO.getImageUrl() == null || sliderDTO.getImageUrl().trim().isEmpty()) {
            throw new BusinessException("Image URL is required");
        }
        if (sliderDTO.getImageUrl().length() > 500) {
            throw new BusinessException("Image URL must not exceed 500 characters");
        }
        if (sliderDTO.getPosition() == null || sliderDTO.getPosition() < 0) {
            throw new BusinessException("Position must be greater than or equal to 0");
        }
        if (sliderDTO.getAlignment() != null) {
            String alignment = sliderDTO.getAlignment();
            if (!alignment.equals("text-left") && !alignment.equals("text-center") && !alignment.equals("text-right")) {
                throw new BusinessException("Alignment must be one of: text-left, text-center, text-right");
            }
        }
    }

    private SliderDTO convertToDTO(Slider slider) {
        SliderDTO dto = new SliderDTO();
        dto.setId(slider.getUuid());
        dto.setDescriptionEn(slider.getDescriptionEn());
        dto.setDescriptionTh(slider.getDescriptionTh());
        dto.setImageUrl(slider.getImageUrl());
        
        try {
            String publicUrl = awsService.getPublicUrl("slider", slider.getImageUrl());
            dto.setImagePreviewUrl(publicUrl);
        } catch (Exception e) {
            log.warn("Failed to generate preview URL for slider {}: {}", slider.getUuid(), e.getMessage());
            dto.setImagePreviewUrl(null);
        }
        
        dto.setAlignment(slider.getAlignment());
        dto.setPosition(slider.getPosition());
        dto.setActive(slider.getActive());

        return dto;
    }
}
