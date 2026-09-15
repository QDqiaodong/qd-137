package com.example.balloon.repository;

import com.example.balloon.entity.Bracket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BracketRepository extends JpaRepository<Bracket, Long> {

    Optional<Bracket> findByBracketCode(String bracketCode);

    List<Bracket> findByStatus(String status);

    /**
     * 按当日风速区间筛支架：只返回区间有重叠的支架（对不上的不进结果），
     * 完全盖住目标区间的排在前面，只盖住一部分的排在后面。
     */
    @Query("SELECT b FROM Bracket b WHERE b.status = 'ACTIVE' " +
           "AND b.minWindSpeed <= :maxWindSpeed AND b.maxWindSpeed >= :minWindSpeed " +
           "ORDER BY CASE WHEN b.minWindSpeed <= :minWindSpeed AND b.maxWindSpeed >= :maxWindSpeed THEN 0 ELSE 1 END, " +
           "b.maxWindSpeed DESC, b.minWindSpeed ASC, b.id ASC")
    List<Bracket> findSuitableBrackets(@Param("minWindSpeed") Double minWindSpeed,
                                        @Param("maxWindSpeed") Double maxWindSpeed);

    @Query("SELECT b FROM Bracket b WHERE b.status = 'ACTIVE' " +
           "AND b.maxWindSpeed >= :windSpeed")
    List<Bracket> findByMaxWindSpeedGreaterThanEqual(@Param("windSpeed") Double windSpeed);

    @Query("SELECT b FROM Bracket b WHERE b.status = 'ACTIVE' " +
           "AND b.minWindSpeed >= :minWind AND b.maxWindSpeed <= :maxWind")
    List<Bracket> findByWindRange(@Param("minWind") Double minWind, @Param("maxWind") Double maxWind);

    List<Bracket> findByBracketType(String bracketType);

    @Query("SELECT DISTINCT b.bracketType FROM Bracket b")
    List<String> findDistinctBracketTypes();
}
