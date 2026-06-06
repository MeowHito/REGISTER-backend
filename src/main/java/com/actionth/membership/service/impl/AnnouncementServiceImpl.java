package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Announcement;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.MediaFile;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.model.request.AnnouncementDTO;
import com.actionth.membership.model.request.MediaFileDTO;
import com.actionth.membership.repository.AnnouncementRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.MediaFileRepository;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.AnnouncementService;
import com.actionth.membership.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    private final EventRepository eventRepository;

    private final MediaFileRepository mediaFileRepository;

    private final AWSService awsService;

    private final ModelMapper modelMapper;

    private final ObjectMapper mapper;

    private final UserService userService;

    @Override
    public Page<AnnouncementDTO> findAll(PagingData pagingData) {
        User user = userService.getCurrentUserSession();

        if (user == null) {
            return Page.empty();
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        boolean isAdmin = user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());

        Specification<Announcement> spec = (root, query, cb) -> {
            query.distinct(true);
            Join<Announcement, Event> event = root.join("event", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin) {
                Subquery<Integer> permSub = query.subquery(Integer.class);
                Root<EventPermission> epRoot = permSub.from(EventPermission.class);
                permSub.select(epRoot.get("event").get("id"));
                permSub.where(
                        cb.equal(epRoot.get("user").get("id"), user.getId()),
                        cb.isTrue(epRoot.get("active")));

                predicates.add(cb.or(
                        cb.equal(event.get("organizer").get("id"), user.getId()),
                        event.get("id").in(permSub)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                    .like(root.get(pagingData.getSearchField()), "%" + pagingData.getSearchText() + "%"));
        }
        Page<Announcement> announcements = announcementRepository.findAll(spec, pageable);

        return announcements.map(announcement -> {
            AnnouncementDTO dto = modelMapper.map(announcement, AnnouncementDTO.class);
            dto.setId(announcement.getUuid());
            dto.setEventId(announcement.getEvent().getUuid());
            return dto;
        });
    }

    @Override
    public AnnouncementDTO findByUuid(String uuid) {
        Announcement announcement = announcementRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        AnnouncementDTO dto = modelMapper.map(announcement, AnnouncementDTO.class);
        OffsetDateTime createdDate = announcement.getCreatedTime();

        dto.setId(announcement.getUuid());
        dto.setEventId(announcement.getEvent().getUuid());
        dto.setCreateDate(createdDate);

        List<MediaFile> mediaFiles = mediaFileRepository.findAllByPrefixPathAndRefId("announcement",
                announcement.getId());

        List<MediaFileDTO> mediaFileDTOList = new ArrayList<>();

        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MediaFile media : mediaFiles) {
                MediaFileDTO mediaDto = new MediaFileDTO();
                mediaDto.setId(media.getUuid());
                mediaDto.setPrefixPath(media.getPrefixPath());
                mediaDto.setPath(media.getPath());

                if (media.getPrefixPath() != null && media.getPath() != null) {
                    try {
                        String publicUrl = awsService.getPublicUrl(media.getPrefixPath(), media.getPath());
                        mediaDto.setThumbUrl(publicUrl); // เก็บ public URL
                    } catch (Exception e) {
                        log.error("Error generating public URL for media: {}", e.getMessage());
                    }
                }

                mediaFileDTOList.add(mediaDto);
            }
        }

        // set mediaFiles เข้า dto
        dto.setMediaFiles(mediaFileDTOList);

        return dto;
    }

    @Override
    public long countUnreadAnnouncements() {
        User user = userService.getCurrentUserSession();
        if (user == null) {
            return 0;
        }
        boolean isAdmin = user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());
        if (isAdmin) {
            return announcementRepository.countUnread();
        }
        return announcementRepository.countUnreadByUser(user.getId());
    }

    @Override
    public void createAnnouncement(AnnouncementDTO announcementDTO) {
        int count = announcementRepository.countByEventUuid(announcementDTO.getEventId());

        if (count >= 5) {
            throw new BusinessException("ไม่สามารถฝากข่าวได้เกิน 5 รายการต่ออีเว้นท์");
        }

        Announcement announcement = mapper.convertValue(announcementDTO, Announcement.class);

        Event event = eventRepository.findByUuid(announcementDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        announcement.setEvent(event);

        announcementRepository.save(announcement);

        // เพิ่มรูปที่แนบมากับ announcement
        if (announcementDTO.getMediaFiles() != null && !announcementDTO.getMediaFiles().isEmpty()) {
            List<MediaFile> mediaFiles = announcementDTO.getMediaFiles().stream()
                    .map(dto -> {
                        MediaFile file = new MediaFile();
                        file.setPath(dto.getPath());
                        file.setPrefixPath(announcementDTO.getPrefixPath());
                        file.setRefId(announcement.getId());
                        return file;
                    })
                    .toList();

            mediaFileRepository.saveAll(mediaFiles);
        }

    }

    @Override
    public void updateAnnouncement(AnnouncementDTO announcementDTO) {
        int count = announcementRepository.countByEventUuidExcludingUuid(
                announcementDTO.getEventId(),
                announcementDTO.getId());

        if (count >= 5) {
            throw new BusinessException("ไม่สามารถฝากข่าวได้เกิน 5 รายการต่ออีเว้นท์");
        }

        Announcement announcement = announcementRepository.findByUuid(announcementDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        mapAnnouncement(announcementDTO, announcement);

        announcementRepository.save(announcement);

        // แยกรูปที่มี uuid (เก่า) กับ ไม่มี uuid (ใหม่)
        List<MediaFileDTO> files = Optional.ofNullable(announcementDTO.getMediaFiles()).orElse(Collections.emptyList());

        List<String> keepIds = files.stream()
                .map(MediaFileDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        List<MediaFileDTO> newFiles = files.stream()
                .filter(f -> f.getId() == null)
                .toList();

        // ลบรูปที่ไม่ได้ส่งกลับมา
        List<MediaFile> oldFiles = mediaFileRepository.findAllByPrefixPathAndRefId(
                announcementDTO.getPrefixPath(), announcement.getId());

        List<MediaFile> toDelete = oldFiles.stream()
                .filter(f -> !keepIds.contains(f.getUuid()))
                .toList();

        mediaFileRepository.deleteAll(toDelete);

        // เพิ่มรูปใหม่
        List<MediaFile> toAdd = newFiles.stream()
                .map(f -> {
                    MediaFile m = new MediaFile();
                    m.setRefId(announcement.getId());
                    m.setPrefixPath(announcementDTO.getPrefixPath());
                    m.setPath(f.getPath());
                    return m;
                }).toList();

        mediaFileRepository.saveAll(toAdd);

    }

    private void mapAnnouncement(AnnouncementDTO dto, Announcement announcement) {
        announcement.setUuid(dto.getId());
        announcement.setTitle(dto.getTitle());
        announcement.setDetail(dto.getDetail());
        announcement.setPrefixPath(dto.getPrefixPath());
        announcement.setIsRead(dto.getIsRead());
        announcement.setStartDate(dto.getStartDate());

        if (dto.getEventId() != null) {
            Event event = eventRepository.findByUuid(dto.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            announcement.setEvent(event);
        }
    }

    @Override
    public void updateAnnouncementReadStatus(AnnouncementDTO announcementDTO) {

        Announcement announcement = announcementRepository.findByUuid(announcementDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));

        announcement.setIsRead(announcementDTO.getIsRead());

        announcementRepository.save(announcement);
    }

    @Override
    public void deleteAnnouncement(String uuid, String mode) {
        if ("hard".equals(mode)) {
            Announcement entity = announcementRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));
            announcementRepository.delete(entity);
        } else if ("soft".equals(mode)) {
            Announcement entity = announcementRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Announcement not found"));
            entity.setActive(false);
            announcementRepository.save(entity);
        }
    }
}
