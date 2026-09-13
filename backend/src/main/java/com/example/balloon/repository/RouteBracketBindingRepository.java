package com.example.balloon.repository;

import com.example.balloon.entity.RouteBracketBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteBracketBindingRepository extends JpaRepository<RouteBracketBinding, Long> {

    List<RouteBracketBinding> findByRouteId(Long routeId);

    List<RouteBracketBinding> findByBracketId(Long bracketId);

    Optional<RouteBracketBinding> findByRouteIdAndBracketId(Long routeId, Long bracketId);

    @Query("SELECT b FROM RouteBracketBinding b WHERE b.route.id = :routeId AND b.status = 'ACTIVE'")
    List<RouteBracketBinding> findActiveByRouteId(@Param("routeId") Long routeId);

    @Query("SELECT b FROM RouteBracketBinding b WHERE b.bracket.id = :bracketId AND b.status = 'ACTIVE'")
    List<RouteBracketBinding> findActiveByBracketId(@Param("bracketId") Long bracketId);

    long countByRouteIdAndStatus(Long routeId, String status);
}
