package com.actionth.membership.service.impl;

import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
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
import com.actionth.membership.model.Contract;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.MediaFile;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.model.request.ContractDTORequest;
import com.actionth.membership.model.request.MediaFileDTO;
import com.actionth.membership.repository.ContractRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.MediaFileRepository;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.ContractService;
import com.actionth.membership.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ContractServiceImpl implements ContractService {
    private final ContractRepository contractRepository;

    private final EventRepository eventRepository;

    private final AWSService awsService;

    private final ModelMapper modelMapper;

    private final MediaFileRepository mediaFileRepository;

    private final UserService userService;

    @Override
    public Page<ContractDTORequest> findAll(PagingData pagingData) {
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

        Specification<Contract> spec = (root, query, criteriaBuilder) -> {
            query.distinct(true);
            Join<Contract, Event> event = root.join("event", JoinType.LEFT);
            Join<Event, User> organizer = event.join("organizer", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("active"), true));

            if (user != null && user.getRole() != null) {
                String role = user.getRole().getRole();
                if ("organizer".equals(role)) {
                    predicates.add(criteriaBuilder.equal(organizer.get("id"), user.getId()));
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                    .like(root.get(pagingData.getSearchField()), "%" + pagingData.getSearchText() + "%"));
        }
        Page<Contract> contracts = contractRepository.findAll(spec, pageable);

        return contracts.map(contract -> {
            ContractDTORequest dto = modelMapper.map(contract, ContractDTORequest.class);
            dto.setId(contract.getUuid());
            dto.setEventId(contract.getEvent().getUuid());
            return dto;
        });
    }

    @Override
    public ContractDTORequest findByUuid(String uuid) {
        Contract contract = contractRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        ContractDTORequest dto = modelMapper.map(contract, ContractDTORequest.class);
        dto.setId(contract.getUuid());
        dto.setEventId(contract.getEvent().getUuid());

        if (dto.getPrefixPath() != null && !dto.getPrefixPath().isEmpty()) {
            try {
                String publicUrl = awsService.getPublicUrl(dto.getPrefixPath(), dto.getContractPath());
                String customerSignaturePublicUrl = awsService.getPublicUrl(dto.getPrefixPath(),
                        dto.getCustomerSignature());
                String tempCertificatePath = awsService.getPublicUrl(dto.getPrefixPath(), dto.getCertificatePath());
                String tempIdCardPath = awsService.getPublicUrl(dto.getPrefixPath(), dto.getIdCardPath());
                String tempBankAccountPath = awsService.getPublicUrl(dto.getPrefixPath(), dto.getBankAccountPath());
                String tempPowerOfAttorneyPath = awsService.getPublicUrl(dto.getPrefixPath(),
                        dto.getPowerOfAttorneyPath());
                String tempPp20Path = awsService.getPublicUrl(dto.getPrefixPath(), dto.getPp20Path());
                String tempOtherDocumentPath = awsService.getPublicUrl(dto.getPrefixPath(), dto.getOtherDocumentPath());

                dto.setTempContractPath(publicUrl);
                dto.setThumbCustomerSignaturePath(customerSignaturePublicUrl);
                dto.setTempCertificatePath(tempCertificatePath);
                dto.setTempIdCardPath(tempIdCardPath);
                dto.setTempBankAccountPath(tempBankAccountPath);
                dto.setTempPowerOfAttorneyPath(tempPowerOfAttorneyPath);
                dto.setTempPp20Path(tempPp20Path);
                dto.setTempOtherDocumentPath(tempOtherDocumentPath);
            } catch (SQLException e) {
                log.error("Unable to generate public URL: {}", e.getMessage());
            }
        }
        List<MediaFile> mediaFiles = mediaFileRepository.findAllByPrefixPathAndRefId("contract",
                contract.getId());

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
                        mediaDto.setThumbUrl(publicUrl);
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

    public String generateRunNo() {
        String prefix = "QT";
        String year = String.valueOf(Year.now().getValue());

        String lastRunNo = contractRepository.findLastRunNo(year);

        int nextRunningNo = 1;
        if (lastRunNo != null) {
            String lastRunningNoStr = lastRunNo.substring(prefix.length() + year.length());
            nextRunningNo = Integer.parseInt(lastRunningNoStr) + 1;
        }

        String runningNo = String.format("%04d", nextRunningNo);

        return prefix + year + runningNo;
    }

    @Override
    public void createContract(ContractDTORequest contractDTO) {
        Contract contract = new Contract();

        Event event = eventRepository.findByUuid(contractDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        String runNo = generateRunNo();

        contract.setRunNo(runNo);
        contract.setEvent(event);

        mapContractDetails(contractDTO, contract);

        contractRepository.save(contract);

        // เพิ่มรูปที่แนบมากับ announcement
        if (contractDTO.getMediaFiles() != null && !contractDTO.getMediaFiles().isEmpty()) {
            List<MediaFile> mediaFiles = contractDTO.getMediaFiles().stream()
                    .map(dto -> {
                        MediaFile file = new MediaFile();
                        file.setPath(dto.getPath());
                        file.setPrefixPath(contractDTO.getPrefixPath());
                        file.setRefId(contract.getId());
                        return file;
                    })
                    .toList();

            mediaFileRepository.saveAll(mediaFiles);
        }
    }

    private void mapContractDetails(ContractDTORequest dto, Contract contract) {
        contract.setOrganizerName(dto.getOrganizerName());
        contract.setIdNo(dto.getIdNo());
        contract.setTaxNo(dto.getTaxNo());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setBankbook(dto.getBankbook());
        contract.setAccountNo(dto.getAccountNo());
        contract.setAccountName(dto.getAccountName());
        contract.setEmail(dto.getEmail());
        contract.setTel(dto.getTel());
        contract.setAddress(dto.getAddress());
        contract.setProvince(dto.getProvince());
        contract.setAmphoe(dto.getAmphoe());
        contract.setDistrict(dto.getDistrict());
        contract.setZipcode(dto.getZipcode());
        contract.setRemark(dto.getRemark());
        contract.setPrefixPath(dto.getPrefixPath());
        contract.setCertificatePath(dto.getCertificatePath());
        contract.setIdCardPath(dto.getIdCardPath());
        contract.setBankAccountPath(dto.getBankAccountPath());
        contract.setPowerOfAttorneyPath(dto.getPowerOfAttorneyPath());
        contract.setPp20Path(dto.getPp20Path());
        contract.setOtherDocumentPath(dto.getOtherDocumentPath());
    }

    @Override
    public void updateContract(ContractDTORequest contractDTO) {

        Contract contract = contractRepository.findByUuid(contractDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        mapContractDetails(contractDTO, contract);

        if (contractDTO.getEventId() != null) {
            Event event = eventRepository.findByUuid(contractDTO.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            contract.setEvent(event);
        }

        contractRepository.save(contract);

        // แยกรูปที่มี uuid (เก่า) กับ ไม่มี uuid (ใหม่)
        List<MediaFileDTO> files = Optional.ofNullable(contractDTO.getMediaFiles()).orElse(Collections.emptyList());

        List<String> keepIds = files.stream()
                .map(MediaFileDTO::getId)
                .filter(Objects::nonNull)
                .toList();

        List<MediaFileDTO> newFiles = files.stream()
                .filter(f -> f.getId() == null)
                .toList();

        // ลบรูปที่ไม่ได้ส่งกลับมา
        List<MediaFile> oldFiles = mediaFileRepository.findAllByPrefixPathAndRefId(
                contractDTO.getPrefixPath(), contract.getId());

        List<MediaFile> toDelete = oldFiles.stream()
                .filter(f -> !keepIds.contains(f.getUuid()))
                .toList();

        mediaFileRepository.deleteAll(toDelete);

        // เพิ่มรูปใหม่
        List<MediaFile> toAdd = newFiles.stream()
                .map(f -> {
                    MediaFile m = new MediaFile();
                    m.setRefId(contract.getId());
                    m.setPrefixPath(contractDTO.getPrefixPath());
                    m.setPath(f.getPath());
                    return m;
                }).toList();

        mediaFileRepository.saveAll(toAdd);
    }

    @Override
    public void updateContractDocument(ContractDTORequest contractDTO) {

        Contract contract = contractRepository.findByUuid(contractDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        contract.setPrefixPath(contractDTO.getPrefixPath());
        contract.setContractPath(contractDTO.getContractPath());
        contract.setDetail(contractDTO.getDetail());
        contract.setContractDate(contractDTO.getContractDate());
        contract.setProviderName(contractDTO.getProviderName());
        contract.setProviderPosition(contractDTO.getProviderPosition());
        contract.setCustomerSignature(null);
        contract.setCustomerName(null);
        contract.setCustomerPosition(null);
        contract.setIsUploadContract(false);

        contractRepository.save(contract);
    }

    @Override
    public void updateContractSignature(ContractDTORequest contractDTO) {

        Contract contract = contractRepository.findByUuid(contractDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        contract.setPrefixPath(contractDTO.getPrefixPath());
        contract.setContractPath(contractDTO.getContractPath());
        contract.setCustomerSignature(contractDTO.getCustomerSignature());
        contract.setCustomerName(contractDTO.getCustomerName());
        contract.setCustomerPosition(contractDTO.getCustomerPosition());
        contract.setIsUploadContract(contractDTO.getIsUploadContract());

        contractRepository.save(contract);
    }

    @Override
    public void deleteContract(String uuid, String mode) {
        if ("hard".equals(mode)) {
            Contract entity = contractRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            contractRepository.delete(entity);
        } else if ("soft".equals(mode)) {
            Contract entity = contractRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            entity.setActive(false);
            contractRepository.save(entity);
        }
    }
}
