package com.example.balloon.repository;

import com.example.balloon.entity.ReleaseCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReleaseCertificateRepository extends JpaRepository<ReleaseCertificate, Long> {

    /** 台账按录入先后展示 */
    List<ReleaseCertificate> findAllByOrderByIdAsc();

    /** 证号撞库校验；撞号时回带目前是谁、工号多少在用 */
    Optional<ReleaseCertificate> findByCertificateNo(String certificateNo);
}
