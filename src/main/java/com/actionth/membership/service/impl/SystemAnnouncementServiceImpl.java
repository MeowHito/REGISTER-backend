package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.SystemAnnouncement;
import com.actionth.membership.model.request.SystemAnnouncementDTO;
import com.actionth.membership.repository.SystemAnnouncementRepository;
import com.actionth.membership.service.SystemAnnouncementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SystemAnnouncementServiceImpl implements SystemAnnouncementService {

    private final SystemAnnouncementRepository repository;
    private final ModelMapper modelMapper;

    @Override
    public List<SystemAnnouncementDTO> findAllActive() {
        OffsetDateTime now = OffsetDateTime.now();
        return repository.findAllCurrentlyActive(now).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public Page<SystemAnnouncementDTO> findAll(PagingData pagingData) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (pagingData != null && pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        int page = pagingData != null ? pagingData.getPage() : 0;
        int size = pagingData != null && pagingData.getSize() > 0 ? pagingData.getSize() : 20;
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<SystemAnnouncement> spec = Specification.where(null);

        if (pagingData != null && pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, cb) -> cb.like(
                    root.get(pagingData.getSearchField()),
                    "%" + pagingData.getSearchText() + "%"));
        }

        return repository.findAll(spec, pageable).map(this::toDTO);
    }

    @Override
    public SystemAnnouncementDTO findByUuid(String uuid) {
        SystemAnnouncement entity = repository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("System announcement not found"));
        return toDTO(entity);
    }

    @Override
    public void create(SystemAnnouncementDTO dto) {
        SystemAnnouncement entity = new SystemAnnouncement();
        entity.setTitle(dto.getTitle());
        entity.setMessage(dto.getMessage());
        entity.setType(dto.getType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        repository.save(entity);
    }

    @Override
    public void update(SystemAnnouncementDTO dto) {
        SystemAnnouncement entity = repository.findByUuid(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("System announcement not found"));
        entity.setTitle(dto.getTitle());
        entity.setMessage(dto.getMessage());
        entity.setType(dto.getType());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        if (dto.getActive() != null) {
            entity.setActive(dto.getActive());
        }
        repository.save(entity);
    }

    @Override
    public void delete(String uuid) {
        SystemAnnouncement entity = repository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("System announcement not found"));
        entity.setActive(false);
        repository.save(entity);
    }

    private SystemAnnouncementDTO toDTO(SystemAnnouncement entity) {
        SystemAnnouncementDTO dto = modelMapper.map(entity, SystemAnnouncementDTO.class);
        dto.setId(entity.getUuid());
        dto.setActive(entity.getActive());
        return dto;
    }
}
