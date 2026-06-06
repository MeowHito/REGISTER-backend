package com.actionth.membership.service;

import com.actionth.membership.model.Contract;
import com.actionth.membership.model.ContractLog;
import com.actionth.membership.repository.ContractLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContractLogService {

    @Autowired
    private ContractLogRepository contractLogRepository;

    public void saveContractLog(Contract contract, String actionType) {
        ContractLog log = new ContractLog();
        log.setActionType(actionType);
        log.setContractId(contract.getId());
        log.setRunNo(contract.getRunNo());
        log.setEventId(contract.getEvent().getId());
        log.setOrganizerName(contract.getOrganizerName());
        log.setIdNo(contract.getIdNo());
        log.setTaxNo(contract.getTaxNo());
        log.setStartDate(contract.getStartDate());
        log.setEndDate(contract.getEndDate());
        log.setBankbook(contract.getBankbook());
        log.setAccountNo(contract.getAccountNo());
        log.setAccountName(contract.getAccountName());
        log.setEmail(contract.getEmail());
        log.setTel(contract.getTel());
        log.setAddress(contract.getAddress());
        log.setProvince(contract.getProvince());
        log.setAmphoe(contract.getAmphoe());
        log.setDistrict(contract.getDistrict());
        log.setZipcode(contract.getZipcode());
        log.setPrefixPath(contract.getPrefixPath());
        log.setContractPath(contract.getContractPath());
        log.setCertificatePath(contract.getCertificatePath());
        log.setIdCardPath(contract.getIdCardPath());
        log.setBankAccountPath(contract.getBankAccountPath());
        log.setPowerOfAttorneyPath(contract.getPowerOfAttorneyPath());
        log.setPp20Path(contract.getPp20Path());
        log.setOtherDocumentPath(contract.getOtherDocumentPath());
        log.setRemark(contract.getRemark());
        log.setDetail(contract.getDetail());
        log.setContractDate(contract.getContractDate());
        log.setProviderSignature(contract.getProviderSignature());
        log.setProviderSeal(contract.getProviderSeal());
        log.setProviderName(contract.getProviderName());
        log.setProviderPosition(contract.getProviderPosition());
        log.setCustomerSignature(contract.getCustomerSignature());
        log.setCustomerSeal(contract.getCustomerSeal());
        log.setCustomerName(contract.getCustomerName());
        log.setCustomerPosition(contract.getCustomerPosition());
        log.setIsUploadContract(contract.getIsUploadContract());

        contractLogRepository.save(log);
    }
}


