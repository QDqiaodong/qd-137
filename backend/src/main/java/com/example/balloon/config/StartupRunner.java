package com.example.balloon.config;

import com.example.balloon.cache.BracketCacheService;
import com.example.balloon.entity.Bracket;
import com.example.balloon.entity.Route;
import com.example.balloon.entity.RouteBracketBinding;
import com.example.balloon.repository.BracketRepository;
import com.example.balloon.repository.RouteBracketBindingRepository;
import com.example.balloon.repository.RouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupRunner implements CommandLineRunner {

    private final BracketCacheService bracketCacheService;
    private final BracketRepository bracketRepository;
    private final RouteRepository routeRepository;
    private final RouteBracketBindingRepository bindingRepository;

    @Override
    public void run(String... args) throws Exception {
        seedInitialData();
        log.info("Initializing bracket cache...");
        bracketCacheService.initCache();
        log.info("Bracket cache initialization completed.");
    }

    private void seedInitialData() {
        if (bracketRepository.count() == 0) {
            bracketRepository.saveAll(List.of(
                    Bracket.builder()
                            .bracketCode("BRK-001")
                            .bracketName("草坪固定地锚支架")
                            .maxLoad(850.0)
                            .minWindSpeed(0.0)
                            .maxWindSpeed(6.0)
                            .bracketType("地面固定支架")
                            .status("ACTIVE")
                            .description("适用于观光起降草坪的基础固定场景")
                            .build(),
                    Bracket.builder()
                            .bracketCode("BRK-002")
                            .bracketName("配重式移动支架")
                            .maxLoad(1100.0)
                            .minWindSpeed(2.0)
                            .maxWindSpeed(9.0)
                            .bracketType("配重支架")
                            .status("ACTIVE")
                            .description("适用于临时活动场地和中等风力区间")
                            .build(),
                    Bracket.builder()
                            .bracketCode("BRK-003")
                            .bracketName("强风区加固支架")
                            .maxLoad(1500.0)
                            .minWindSpeed(5.0)
                            .maxWindSpeed(12.0)
                            .bracketType("大风专用支架")
                            .status("ACTIVE")
                            .description("适用于高原训练航线和强风起降区")
                            .build(),
                    Bracket.builder()
                            .bracketCode("BRK-004")
                            .bracketName("培训区轻型支架")
                            .maxLoad(650.0)
                            .minWindSpeed(0.0)
                            .maxWindSpeed(4.5)
                            .bracketType("培训支架")
                            .status("ACTIVE")
                            .description("适用于低风速教学演示与新手培训")
                            .build()
            ));
            log.info("Seeded initial bracket data");
        }

        if (routeRepository.count() == 0) {
            routeRepository.saveAll(List.of(
                    Route.builder()
                            .routeCode("RTE-001")
                            .routeName("湖畔晨曦观光航线")
                            .groupName("观光航线")
                            .minWindSpeed(1.0)
                            .maxWindSpeed(5.5)
                            .distance(4.8)
                            .duration(35)
                            .difficultyLevel("初级")
                            .status("ACTIVE")
                            .description("适合亲子和初次体验游客的低风速观光航线")
                            .build(),
                    Route.builder()
                            .routeCode("RTE-002")
                            .routeName("山谷穿越训练航线")
                            .groupName("培训航线")
                            .minWindSpeed(2.5)
                            .maxWindSpeed(8.0)
                            .distance(7.2)
                            .duration(50)
                            .difficultyLevel("中级")
                            .status("ACTIVE")
                            .description("用于飞行员训练的中等风力区间航线")
                            .build(),
                    Route.builder()
                            .routeCode("RTE-003")
                            .routeName("高原挑战竞赛航线")
                            .groupName("竞赛航线")
                            .minWindSpeed(5.0)
                            .maxWindSpeed(11.0)
                            .distance(10.5)
                            .duration(70)
                            .difficultyLevel("高级")
                            .status("ACTIVE")
                            .description("面向赛事活动的强风区间航线")
                            .build()
            ));
            log.info("Seeded initial route data");
        }

        if (bindingRepository.count() == 0) {
            Route sightseeingRoute = routeRepository.findByRouteCode("RTE-001").orElse(null);
            Route trainingRoute = routeRepository.findByRouteCode("RTE-002").orElse(null);
            Route contestRoute = routeRepository.findByRouteCode("RTE-003").orElse(null);
            Bracket grassBracket = bracketRepository.findByBracketCode("BRK-001").orElse(null);
            Bracket counterweightBracket = bracketRepository.findByBracketCode("BRK-002").orElse(null);
            Bracket highWindBracket = bracketRepository.findByBracketCode("BRK-003").orElse(null);

            if (sightseeingRoute != null && trainingRoute != null && contestRoute != null
                    && grassBracket != null && counterweightBracket != null && highWindBracket != null) {
                bindingRepository.saveAll(List.of(
                        RouteBracketBinding.builder()
                                .route(sightseeingRoute)
                                .bracket(grassBracket)
                                .matchLevel("HIGH")
                                .launchWindSpeed(3.0)
                                .windStatus("MATCHED")
                                .status("ACTIVE")
                                .build(),
                        RouteBracketBinding.builder()
                                .route(trainingRoute)
                                .bracket(counterweightBracket)
                                .matchLevel("HIGH")
                                .launchWindSpeed(5.0)
                                .windStatus("MATCHED")
                                .status("ACTIVE")
                                .build(),
                        RouteBracketBinding.builder()
                                .route(contestRoute)
                                .bracket(highWindBracket)
                                .matchLevel("HIGH")
                                .launchWindSpeed(7.0)
                                .windStatus("MATCHED")
                                .status("ACTIVE")
                                .build()
                ));
                log.info("Seeded initial route-bracket bindings");
            }
        }
    }
}
