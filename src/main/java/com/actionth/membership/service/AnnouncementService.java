package com.actionth.membership.service;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.AnnouncementDTO;
import org.springframework.data.domain.Page;

public interface AnnouncementService {

    Page<AnnouncementDTO> findAll(PagingData pagingData);

    AnnouncementDTO findByUuid(String uuid);

    long countUnreadAnnouncements();

    void createAnnouncement(AnnouncementDTO announcementDTO);

    void updateAnnouncement(AnnouncementDTO announcementDTO);

    void updateAnnouncementReadStatus(AnnouncementDTO announcementDTO);

    void deleteAnnouncement(String uuid, String mode);
}
