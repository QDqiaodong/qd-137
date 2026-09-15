package com.example.balloon.repository;

import com.example.balloon.entity.RouteDailyFlight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteDailyFlightRepository extends JpaRepository<RouteDailyFlight, Long> {

    Optional<RouteDailyFlight> findByRouteIdAndFlightDate(Long routeId, LocalDate flightDate);

    List<RouteDailyFlight> findByFlightDate(LocalDate flightDate);

    /**
     * 原子累加：同一天同一条航线已存在记录则在原行上累加趟次与时长，
     * 不存在才插入新行（依赖 uk_route_date 唯一约束）。
     */
    @Modifying
    @Query(value = "INSERT INTO route_daily_flight (route_id, flight_date, flight_count, total_duration, updated_at) "
            + "VALUES (:routeId, :flightDate, :count, :duration, NOW()) "
            + "ON DUPLICATE KEY UPDATE "
            + "flight_count = flight_count + :count, "
            + "total_duration = total_duration + :duration, "
            + "updated_at = NOW()", nativeQuery = true)
    int upsertIncrement(@Param("routeId") Long routeId,
                        @Param("flightDate") LocalDate flightDate,
                        @Param("count") Integer count,
                        @Param("duration") Integer duration);
}
