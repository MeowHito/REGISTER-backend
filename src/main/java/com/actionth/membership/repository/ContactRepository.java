package com.actionth.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.Contact;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Integer>{

}
