package com.example.balloon.repository;

import com.example.balloon.entity.DutyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DutyAssignmentRepository extends JpaRepository<DutyAssignment, Long> {

    List<DutyAssignment> findByOperatorId(Long operatorId);

    boolean existsByOperatorIdAndRouteId(Long operatorId, Long routeId);

    @Query("SELECT d.route.id FROM DutyAssignment d WHERE d.operator.id = :operatorId")
    List<Long> findRouteIdsByOperatorId(@Param("operatorId") Long operatorId);
}
