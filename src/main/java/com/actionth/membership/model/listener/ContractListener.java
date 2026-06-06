package com.actionth.membership.model.listener;

import com.actionth.membership.model.Contract;
import com.actionth.membership.service.ContractLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.*;

@Component
public class ContractListener {

    private static ContractLogService contractLogService;

    @Autowired
    public void setContractLogService(ContractLogService service) {
        contractLogService = service;
    }

    @PrePersist
    public void beforeSave(Contract contract) {
        contractLogService.saveContractLog(contract, "CREATE");
    }

    @PreUpdate
    public void beforeUpdate(Contract contract) {
        contractLogService.saveContractLog(contract, "UPDATE");
    }

    @PreRemove
    public void beforeDelete(Contract contract) {
        contractLogService.saveContractLog(contract, "DELETE");
    }
}
