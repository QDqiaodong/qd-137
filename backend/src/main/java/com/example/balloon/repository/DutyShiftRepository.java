package com.example.balloon.repository;

import com.example.balloon.entity.DutyShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DutyShiftRepository extends JpaRepository<DutyShift, Long> {

    /** 全班组同时只允许一个进行中的班组 */
    Optional<DutyShift> findFirstByShiftStatusOrderByStartedAtDesc(String shiftStatus);

    /** 最近一个班组（无论状态），用于交接间隙找上一班 */
    Optional<DutyShift> findFirstByOrderByStartedAtDesc();
}
