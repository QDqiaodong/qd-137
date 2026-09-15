package com.example.balloon.repository;

import com.example.balloon.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByRouteCode(String routeCode);

    List<Route> findByStatus(String status);

    List<Route> findByGroupNameAndStatus(String groupName, String status);

    @Query("SELECT DISTINCT r.groupName FROM Route r WHERE r.status = 'ACTIVE'")
    List<String> findDistinctGroupNames();

    @Query("SELECT r FROM Route r WHERE r.status = 'ACTIVE'")
    List<Route> findAllActive();
}
