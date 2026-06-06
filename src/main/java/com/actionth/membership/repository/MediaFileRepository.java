package com.actionth.membership.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.MediaFile;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Integer> {
    List<MediaFile> findAllByPrefixPathAndRefId(String prefixPath, Integer refId);

}

