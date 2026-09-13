package com.example.balloon.cache;

import com.example.balloon.entity.Bracket;
import com.example.balloon.repository.BracketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BracketCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BracketRepository bracketRepository;

    private static final String BRACKET_WIND_SPEED_KEY = "bracket:wind_speed";
    private static final String BRACKET_DATA_KEY = "bracket:data:";

    public void cacheBracket(Bracket bracket) {
        redisTemplate.opsForZSet().add(BRACKET_WIND_SPEED_KEY, bracket.getId().toString(), bracket.getMaxWindSpeed());
        redisTemplate.opsForValue().set(BRACKET_DATA_KEY + bracket.getId(), bracket);
        log.debug("Cached bracket: {} with max wind speed: {}", bracket.getBracketCode(), bracket.getMaxWindSpeed());
    }

    public void removeBracketFromCache(Long bracketId) {
        redisTemplate.opsForZSet().remove(BRACKET_WIND_SPEED_KEY, bracketId.toString());
        redisTemplate.delete(BRACKET_DATA_KEY + bracketId);
        log.debug("Removed bracket {} from cache", bracketId);
    }

    public List<Bracket> getBracketsByMinWindSpeed(Double minWindSpeed) {
        Set<Object> bracketIds = redisTemplate.opsForZSet().rangeByScore(BRACKET_WIND_SPEED_KEY, minWindSpeed, Double.MAX_VALUE);
        if (bracketIds == null || bracketIds.isEmpty()) {
            return bracketRepository.findByMaxWindSpeedGreaterThanEqual(minWindSpeed);
        }
        return bracketIds.stream()
                .map(id -> (Bracket) redisTemplate.opsForValue().get(BRACKET_DATA_KEY + id))
                .filter(bracket -> bracket != null && bracket.getStatus().equals("ACTIVE"))
                .collect(Collectors.toList());
    }

    public void refreshAllBrackets() {
        List<Bracket> brackets = bracketRepository.findByStatus("ACTIVE");
        brackets.forEach(this::cacheBracket);
        log.info("Refreshed {} brackets in cache", brackets.size());
    }

    public void initCache() {
        refreshAllBrackets();
    }
}
