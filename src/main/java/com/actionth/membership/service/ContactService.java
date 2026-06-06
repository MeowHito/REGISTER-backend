package com.actionth.membership.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.actionth.membership.model.Contact;
import com.actionth.membership.model.request.ContactDTO;
import com.actionth.membership.repository.ContactRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ContactService {
    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ObjectMapper mapper;

    public Contact createContact(ContactDTO contactDTO) {
        Contact contact = mapper.convertValue(contactDTO, Contact.class);
        return contactRepository.save(contact);
    }
}
