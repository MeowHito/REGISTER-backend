package com.actionth.membership.service;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.SystemAnnouncementDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SystemAnnouncementService {

    List<SystemAnnouncementDTO> findAllActive();

    Page<SystemAnnouncementDTO> findAll(PagingData pagingData);

    SystemAnnouncementDTO findByUuid(String uuid);

    void create(SystemAnnouncementDTO dto);

    void update(SystemAnnouncementDTO dto);

    void delete(String uuid);
}
