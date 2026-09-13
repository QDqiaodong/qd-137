package com.example.balloon.repository;

import com.example.balloon.entity.WindMatchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WindMatchLogRepository extends JpaRepository<WindMatchLog, Long> {

    List<WindMatchLog> findByRouteIdOrderByCreatedAtDesc(Long routeId);

    List<WindMatchLog> findByRouteCodeOrderByCreatedAtDesc(String routeCode);

    List<WindMatchLog> findByBracketCodeOrderByCreatedAtDesc(String bracketCode);

    List<WindMatchLog> findByActionTypeOrderByCreatedAtDesc(String actionType);

    List<WindMatchLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
}
