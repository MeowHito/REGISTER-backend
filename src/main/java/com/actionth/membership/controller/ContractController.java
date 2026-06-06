package com.actionth.membership.controller;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.ContractDTORequest;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.ContractService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private ObjectMapper mapper;

    @GetMapping
    public Response<Page<ContractDTORequest>> getContractsWithPagination(
            @RequestParam(value = "paging", required = false) String pagingJson) throws JsonProcessingException {
        PagingData paging = null;
        if (pagingJson != null) {
            paging = mapper.readValue(pagingJson, PagingData.class);
        }
        return new Response<>(contractService.findAll(paging), "Contracts retrieved successfully", true);
    }

    @GetMapping("/{id}")
    public Response<ContractDTORequest> getContractById(@PathVariable String id) {
        return new Response<>(contractService.findByUuid(id), "Contract retrieved successfully", true);
    }

    @PostMapping
    public Response<Void> createContract(@RequestBody ContractDTORequest contract) {
        contractService.createContract(contract);
        return new Response<>(null, "Contract created successfully", true);
    }

    @PutMapping()
    public Response<Void> updateContract(@RequestBody ContractDTORequest contract) {
        contractService.updateContract(contract);
        return new Response<>(null, "Contract updated successfully", true);
    }

    @PutMapping("/updateContractDocument")
    public Response<Void> updateContractDocument(@RequestBody ContractDTORequest contract) {
        contractService.updateContractDocument(contract);
        return new Response<>(null, "Contract updated successfully", true);
    }

    @PutMapping("/updateContractSignature")
    public Response<Void> updateContractSignature(@RequestBody ContractDTORequest contract) {
        contractService.updateContractSignature(contract);
        return new Response<>(null, "Contract updated successfully", true);
    }

    @DeleteMapping("/{uuid}")
    public Response<Void> deleteContract(@PathVariable String uuid, @RequestParam(value = "mode") String mode) {
        contractService.deleteContract(uuid, mode);
        return new Response<>(null, "Contract deleted successfully", true);
    }
}