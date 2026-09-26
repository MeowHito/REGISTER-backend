package com.actionth.membership.service.impl;

import com.actionth.membership.model.dto.ContractDocumentDTO;
import org.springframework.security.access.AccessDeniedException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.ValidationException;
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
import com.actionth.membership.service.AppConfigService;
import com.actionth.membership.service.ContractService;
import com.actionth.membership.service.ReportService;
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

    private final ReportService reportService;

    private final AppConfigService appConfigService;

    @Value("${app.report-path}")
    private String reportPath;

    private static final String CONTRACT_PDF_TEMPLATE = "/Contract.jrxml";
    private static final String CONTRACT_LOGO = "/logo.png";
    private static final String CONTRACT_PREFIX = "contract";
    private static final String USER_PREFIX = "userData";
    private static final DateTimeFormatter CONTRACT_DATE_FMT = DateTimeFormatter
            .ofPattern("d MMMM yyyy", new Locale("th", "TH"))
            .withChronology(IsoChronology.INSTANCE);
    private static final ZoneId BANGKOK = ZoneId.of("Asia/Bangkok");

    private boolean isAdmin(User user) {
        return user != null && user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());
    }

    /**
     * A contract is between Action and the event's organizer, so only that organizer (or an admin)
     * may see or touch it — collaborators on the event are not a party to it.
     */
    private void assertContractAccess(Event event) {
        User user = userService.getCurrentUserSession();
        if (isAdmin(user)) {
            return;
        }
        boolean isOrganizer = user != null && event != null && event.getOrganizer() != null
                && Objects.equals(event.getOrganizer().getId(), user.getId());
        if (!isOrganizer) {
            throw new AccessDeniedException("No permission to access this contract");
        }
    }

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

            if (!isAdmin(user)) {
                predicates.add(criteriaBuilder.equal(organizer.get("id"), user.getId()));
                predicates.add(criteriaBuilder.equal(root.get("isReadyForSign"), true));
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
        assertContractAccess(contract.getEvent());
        ContractDTORequest dto = modelMapper.map(contract, ContractDTORequest.class);
        dto.setId(contract.getUuid());
        dto.setEventId(contract.getEvent().getUuid());

        String prefix = (dto.getPrefixPath() != null && !dto.getPrefixPath().isEmpty())
                ? dto.getPrefixPath()
                : "contract";
        try {
            dto.setTempContractPath(awsService.getPublicUrl(prefix, dto.getContractPath()));
            dto.setThumbCustomerSignaturePath(awsService.getPublicUrl(prefix, dto.getCustomerSignature()));
            dto.setTempCertificatePath(awsService.getPublicUrl(prefix, dto.getCertificatePath()));
            dto.setTempIdCardPath(awsService.getPublicUrl(prefix, dto.getIdCardPath()));
            dto.setTempBankAccountPath(awsService.getPublicUrl(prefix, dto.getBankAccountPath()));
            dto.setTempPowerOfAttorneyPath(awsService.getPublicUrl(prefix, dto.getPowerOfAttorneyPath()));
            dto.setTempPp20Path(awsService.getPublicUrl(prefix, dto.getPp20Path()));
            dto.setTempOtherDocumentPath(awsService.getPublicUrl(prefix, dto.getOtherDocumentPath()));
        } catch (SQLException e) {
            log.error("Unable to generate public URL: {}", e.getMessage());
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
        assertContractAccess(event);

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
        assertContractAccess(contract.getEvent());

        mapContractDetails(contractDTO, contract);

        // Moving a contract to another event is an admin decision.
        if (contractDTO.getEventId() != null && isAdmin(userService.getCurrentUserSession())) {
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
        assertContractAccess(contract.getEvent());

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
        contract.setIsReadyForSign(false);

        contractRepository.save(contract);
    }

    @Override
    public void updateContractSignature(ContractDTORequest contractDTO) {

        Contract contract = contractRepository.findByUuid(contractDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        assertContractAccess(contract.getEvent());

        boolean alreadySigned = (contract.getCustomerSignature() != null && !contract.getCustomerSignature().isEmpty())
                || Boolean.TRUE.equals(contract.getIsUploadContract());

        if (alreadySigned) {
            throw new ValidationException("สัญญานี้ถูกเซ็นแล้ว ไม่สามารถแก้ไขลายเซ็นได้");
        }

        contract.setPrefixPath(contractDTO.getPrefixPath());
        contract.setContractPath(contractDTO.getContractPath());
        contract.setCustomerSignature(contractDTO.getCustomerSignature());
        contract.setCustomerName(contractDTO.getCustomerName());
        contract.setCustomerPosition(contractDTO.getCustomerPosition());
        contract.setIsUploadContract(contractDTO.getIsUploadContract());

        contractRepository.save(contract);
    }

    @Override
    public void regeneratePdf(String uuid) {
        User currentUser = userService.getCurrentUserSession();
        if (currentUser == null
                || currentUser.getRole() == null
                || !"admin".equalsIgnoreCase(currentUser.getRole().getRoleType())) {
            throw new ValidationException("ท่านไม่มีสิทธิ์ในการเข้าถึงข้อมูล");
        }

        Contract contract = contractRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        String contractPath = contract.getContractPath();
        if (contractPath == null || contractPath.isEmpty()) {
            throw new ResourceNotFoundException("Contract has no stored PDF to regenerate");
        }

        String prefix = (contract.getPrefixPath() != null && !contract.getPrefixPath().isEmpty())
                ? contract.getPrefixPath()
                : CONTRACT_PREFIX;

        File logo = null;
        File seal = null;
        File tempPdf = null;
        try {
            Map<String, Object> paramMap = new HashMap<>();

            logo = copyResourceToTemp(reportPath + CONTRACT_LOGO);
            paramMap.put("logo", logo.getAbsolutePath());

            String sealKey = appConfigService.findFirstByName("providerSealPath");
            if (sealKey != null && !sealKey.isEmpty()) {
                seal = copyResourceToTemp(reportPath + sealKey);
                paramMap.put("providerSeal", seal.getAbsolutePath());
            } else {
                paramMap.put("providerSeal", "");
            }

            paramMap.put("providerSignature", resolveSignatureUrl(USER_PREFIX, userService.getApproverSignatureImg()));
            paramMap.put("customerSignature", resolveSignatureUrl(CONTRACT_PREFIX, contract.getCustomerSignature()));

            paramMap.put("runNo", nullToEmpty(contract.getRunNo()));
            paramMap.put("contractDate", formatContractDate(contract.getContractDate()));
            paramMap.put("detail", nullToEmpty(contract.getDetail()));
            paramMap.put("customerCompany", nullToEmpty(contract.getOrganizerName()));
            paramMap.put("organizer", nullToEmpty(contract.getOrganizerName()));
            paramMap.put("tel", nullToEmpty(contract.getTel()));
            paramMap.put("address", buildFullAddress(contract));
            paramMap.put("taxNo", nullToEmpty(contract.getTaxNo()));
            paramMap.put("event", contract.getEvent() != null ? nullToEmpty(contract.getEvent().getName()) : "");
            paramMap.put("providerName", nullToEmpty(contract.getProviderName()));
            paramMap.put("providerPosition", nullToEmpty(contract.getProviderPosition()));
            paramMap.put("customerSeal", nullToEmpty(contract.getCustomerSeal()));
            paramMap.put("customerName", emptyToNull(contract.getCustomerName()));
            paramMap.put("customerPosition", emptyToNull(contract.getCustomerPosition()));

            byte[] pdfBytes = reportService.generateReport(CONTRACT_PDF_TEMPLATE, paramMap);

            tempPdf = new File(System.getProperty("java.io.tmpdir"), contractPath);
            Files.write(tempPdf.toPath(), pdfBytes);
            awsService.uploadFile(prefix, tempPdf, false);
            log.info("Regenerated contract PDF uploaded: {}/{}", prefix, contractPath);
        } catch (Exception e) {
            log.error("Failed to regenerate contract PDF for runNo {}: {}", contract.getRunNo(), e.getMessage(), e);
            throw new IllegalStateException("Failed to regenerate contract PDF: " + e.getMessage(), e);
        } finally {
            deleteQuietly(logo);
            deleteQuietly(seal);
            deleteQuietly(tempPdf);
        }
    }

    private String resolveSignatureUrl(String prefix, String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            return awsService.isOwnBucketUrl(key) ? key : "";
        }
        try {
            String url = awsService.getPublicUrl(prefix, key);
            return url != null ? url : "";
        } catch (Exception e) {
            log.error("Failed to resolve signature URL for {}/{}: {}", prefix, key, e.getMessage());
            return "";
        }
    }

    private String buildFullAddress(Contract contract) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, contract.getAddress());
        appendIfPresent(sb, contract.getDistrict());
        appendIfPresent(sb, contract.getAmphoe());
        appendIfPresent(sb, contract.getProvince());
        appendIfPresent(sb, contract.getZipcode());
        return sb.toString().trim();
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(trimmed);
    }

    private String formatContractDate(OffsetDateTime date) {
        if (date == null) {
            return "";
        }
        return ZonedDateTime.ofInstant(date.toInstant(), BANGKOK).format(CONTRACT_DATE_FMT);
    }

    private File copyResourceToTemp(String classpathResource) throws IOException {
        String tmp = System.getProperty("java.io.tmpdir") + "/temp-" + System.nanoTime() + ".png";
        try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("Resource not found: " + classpathResource);
            }
            Files.copy(in, Paths.get(tmp), StandardCopyOption.REPLACE_EXISTING);
        }
        return new File(tmp);
    }

    private void deleteQuietly(File file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}", file.getAbsolutePath());
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * The preview PDF carries Action's seal and the approver's signature, so it must never be
     * rendered from whatever the client sends. The contract has to exist and be the caller's; an
     * admin may preview the draft terms they are editing, anyone else gets the stored terms and can
     * only supply their own name, position and signature (from our bucket).
     */
    @Override
    public ContractDocumentDTO resolvePreview(ContractDocumentDTO request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new ValidationException("ไม่พบสัญญาที่ต้องการดูตัวอย่าง");
        }
        Contract contract = contractRepository.findByUuid(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        assertContractAccess(contract.getEvent());

        if (!isAdmin(userService.getCurrentUserSession())) {
            request.setRunNo(contract.getRunNo());
            request.setContractDate(formatContractDate(contract.getContractDate()));
            request.setDetail(contract.getDetail());
            request.setCustomerCompany(contract.getOrganizerName());
            request.setOrganizer(contract.getOrganizerName());
            request.setTel(contract.getTel());
            request.setAddress(buildFullAddress(contract));
            request.setTaxNo(contract.getTaxNo());
            request.setEvent(contract.getEvent() != null ? contract.getEvent().getName() : null);
            request.setProviderName(contract.getProviderName());
            request.setProviderPosition(contract.getProviderPosition());
        }
        // Image parameters are loaded by the report engine, so an arbitrary path or URL here would
        // read local files or internal hosts into the PDF.
        request.setCustomerSeal(null);
        request.setProviderSeal(null);
        request.setProviderSignature(null);
        String signature = request.getCustomerSignature();
        if (signature != null && isAbsolute(signature) && !awsService.isOwnBucketUrl(signature)) {
            request.setCustomerSignature(null);
        }
        return request;
    }

    private boolean isAbsolute(String value) {
        String v = value.trim().toLowerCase();
        return v.contains(":") || v.startsWith("/") || v.contains("..");
    }

    @Override
    public void markReadyForSign(String uuid, boolean ready) {
        User currentUser = userService.getCurrentUserSession();
        if (currentUser == null
                || currentUser.getRole() == null
                || !"admin".equalsIgnoreCase(currentUser.getRole().getRoleType())) {
            throw new ValidationException("ท่านไม่มีสิทธิ์ในการเข้าถึงข้อมูล");
        }

        Contract contract = contractRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));

        if (ready && (contract.getContractPath() == null || contract.getContractPath().isEmpty())) {
            throw new ValidationException("ยังไม่มีเอกสารสัญญา กรุณาสร้างเอกสารก่อน");
        }

        contract.setIsReadyForSign(ready);
        contractRepository.save(contract);
    }

    @Override
    public void deleteContract(String uuid, String mode) {
        if ("hard".equals(mode)) {
            Contract entity = contractRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            assertContractAccess(entity.getEvent());
            contractRepository.delete(entity);
        } else if ("soft".equals(mode)) {
            Contract entity = contractRepository.findByUuid(uuid)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            assertContractAccess(entity.getEvent());
            entity.setActive(false);
            contractRepository.save(entity);
        }
    }
}
