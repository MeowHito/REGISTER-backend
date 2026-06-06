package com.actionth.membership.service;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.SliderDTO;
import com.actionth.membership.model.request.SliderReorderDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SliderService {

    List<SliderDTO> findAllActive();

    Page<SliderDTO> findAll(PagingData pagingData);

    SliderDTO findByUuid(String uuid);

    void createSlider(SliderDTO sliderDTO);

    void updateSlider(SliderDTO sliderDTO);

    void deleteSlider(String uuid, String mode);

    void reorderSliders(SliderReorderDTO reorderDTO);
}
