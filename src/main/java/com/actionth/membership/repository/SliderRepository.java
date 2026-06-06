package com.actionth.membership.repository;

import com.actionth.membership.model.Slider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SliderRepository extends JpaRepository<Slider, Integer>, JpaSpecificationExecutor<Slider> {
    
    Optional<Slider> findByUuid(String uuid);
    
    void deleteByUuid(String uuid);
    
    @Query("SELECT s FROM Slider s WHERE s.active = true ORDER BY s.position ASC")
    List<Slider> findAllActiveOrderByPosition();
    
    @Query("SELECT MAX(s.position) FROM Slider s")
    Integer findMaxPosition();
}
