package com.example.balloon.repository;

import com.example.balloon.entity.BracketReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BracketReturnItemRepository extends JpaRepository<BracketReturnItem, Long> {

    List<BracketReturnItem> findByShiftIdOrderByCreatedAtAsc(Long shiftId);

    long countByShiftIdAndReturnStatus(Long shiftId, String returnStatus);

    Optional<BracketReturnItem> findByShiftIdAndBracketId(Long shiftId, Long bracketId);
}
