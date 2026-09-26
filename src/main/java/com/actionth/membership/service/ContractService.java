package com.actionth.membership.service;

import com.actionth.membership.model.dto.ContractDocumentDTO;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.ContractDTORequest;
import org.springframework.data.domain.Page;

public interface ContractService {

    Page<ContractDTORequest> findAll(PagingData pagingData);

    ContractDTORequest findByUuid(String uuid);

    void createContract(ContractDTORequest contractDTO);

    void updateContract(ContractDTORequest contractDTO);

    void updateContractDocument(ContractDTORequest contractDTO);

    void updateContractSignature(ContractDTORequest contractDTO);

    void deleteContract(String uuid, String mode);

    void regeneratePdf(String uuid);

    void markReadyForSign(String uuid, boolean ready);

    ContractDocumentDTO resolvePreview(ContractDocumentDTO request);
}
