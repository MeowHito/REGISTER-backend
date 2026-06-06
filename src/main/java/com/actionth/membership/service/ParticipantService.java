package com.actionth.membership.service;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.ParticipantDTO;
import com.actionth.membership.model.dto.ParticipantDetailDto;
import com.actionth.membership.model.dto.ParticipantViewDto;
import com.actionth.membership.model.request.ParticipantDTORequest;
import com.actionth.membership.model.request.ParticipantUploadDTORequest;

public interface ParticipantService {

    Page<ParticipantDTO> findAll(String eventId, PagingData pagingData);

    List<ParticipantViewDto> checkParticipantName(String eventId, String name);

    Page<ParticipantViewDto> checkParticipant(String eventId, String name, int page, int size);

    ParticipantDetailDto getParticipantDetail(String participantId);

    List<String> getParticipantByEventId(String eventId);

    ParticipantDTO findParticipantByUuid(String uuid);

    List<Map<String, Object>> getAllParticipantDownload(Integer eventId);

    void updateParticipant(ParticipantDTORequest participantDTO);

    void uploadParticipants(List<ParticipantUploadDTORequest> uploadDTORequests);
}
