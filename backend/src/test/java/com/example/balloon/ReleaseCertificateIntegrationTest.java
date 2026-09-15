package com.example.balloon;

import com.example.balloon.cache.BracketCacheService;
import com.example.balloon.config.StartupRunner;
import com.example.balloon.entity.Operator;
import com.example.balloon.repository.OperatorRepository;
import com.example.balloon.repository.ReleaseCertificateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 放飞证照台账端到端测试：真实 HTTP 层 + 真实 Redis 写锁 + 内存 H2 数据库事务。
 * 覆盖：角色收窄、整批回滚、四类行级拦截（工号/日期/撞库/撞批/换证次序）、
 * 单条换证同次序、两人同时交批只成功一批。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReleaseCertificateIntegrationTest {

    private static final String LOCK_KEY = "lock:release-certificate-ledger:write";
    private static final String DISP_CODE = "DISP-001";
    private static final String DISP_PWD = "dispatch123";
    private static final String PILOT_PWD = "pilot123";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OperatorRepository operatorRepository;
    @Autowired
    private ReleaseCertificateRepository certificateRepository;
    @Autowired
    private StringRedisTemplate redis;

    /** 跳过支架/航线/初始证照等启动播种，本测试自行造数 */
    @MockBean
    private StartupRunner startupRunner;
    /** 避免启动时访问支架缓存 */
    @MockBean
    private BracketCacheService bracketCacheService;

    @BeforeEach
    void setUp() {
        certificateRepository.deleteAll();
        operatorRepository.deleteAll();
        redis.delete(LOCK_KEY);

        operatorRepository.save(Operator.builder()
                .operatorCode(DISP_CODE).operatorName("调度·王调")
                .role(Operator.ROLE_DISPATCHER)
                .passwordHash(com.example.balloon.service.OperatorService.sha256(DISP_PWD))
                .status("ACTIVE").build());
        operatorRepository.save(Operator.builder()
                .operatorCode("OP-001").operatorName("放飞员·李帆")
                .role(Operator.ROLE_LAUNCH_OPERATOR)
                .passwordHash(com.example.balloon.service.OperatorService.sha256(PILOT_PWD))
                .status("ACTIVE").build());
        operatorRepository.save(Operator.builder()
                .operatorCode("OP-002").operatorName("放飞员·赵翔")
                .role(Operator.ROLE_LAUNCH_OPERATOR)
                .passwordHash(com.example.balloon.service.OperatorService.sha256(PILOT_PWD))
                .status("ACTIVE").build());
        operatorRepository.save(Operator.builder()
                .operatorCode("OP-003").operatorName("放飞员·停职")
                .role(Operator.ROLE_LAUNCH_OPERATOR)
                .passwordHash(com.example.balloon.service.OperatorService.sha256(PILOT_PWD))
                .status("INACTIVE").build());
    }

    @AfterEach
    void tearDown() {
        redis.delete(LOCK_KEY);
    }

    private Map<String, String> row(String code, String no, String issue, String expire) {
        return Map.of("operatorCode", code, "certificateNo", no, "issueDate", issue, "expireDate", expire);
    }

    private String batchJson(List<Map<String, String>> rows) throws Exception {
        return objectMapper.writeValueAsString(Map.of("rows", rows));
    }

    // ---------- 身份与角色 ----------

    @Test
    void noIdentityIs401() throws Exception {
        mockMvc.perform(get("/api/certificates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pilotCanReadButCannotWrite() throws Exception {
        mockMvc.perform(get("/api/certificates")
                        .header("X-Operator-Code", "OP-001")
                        .header("X-Operator-Password", PILOT_PWD))
                .andExpect(status().isOk());

        String body = batchJson(List.of(row("OP-001", "C-X1", "2025-01-01", "2026-01-01")));
        mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", "OP-001")
                        .header("X-Operator-Password", PILOT_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertEquals(0, certificateRepository.count(), "放飞员越权写入不得落库");
    }

    // ---------- 整批成功 / 整批回滚 ----------

    @Test
    void validBatchSavesAllAndIsPersisted() throws Exception {
        String body = batchJson(List.of(
                row("OP-001", "CERT-A1", "2025-01-01", "2026-01-01"),
                row("OP-002", "CERT-B1", "2025-02-01", "2027-02-01")));
        MvcResult result = mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertEquals(2, json.get("savedCount").asInt());
        assertEquals(2, certificateRepository.count());
        assertTrue(certificateRepository.findByCertificateNo("CERT-A1").isPresent());
    }

    @Test
    void unknownOperatorRejectsWholeBatchAtThatRow() throws Exception {
        String body = batchJson(List.of(
                row("OP-001", "CERT-A1", "2025-01-01", "2026-01-01"),
                row("OP-404", "CERT-Z9", "2025-01-01", "2026-01-01")));
        MvcResult result = submitBatchExpect422(body);
        JsonNode detail = rejectedDetail(result);
        assertEquals(2, detail.get("rowNumber").asInt(), "应卡在第 2 条");
        assertEquals("OPERATOR_NOT_FOUND", detail.get("conflictType").asText());
        assertEquals(0, certificateRepository.count(), "整批必须回滚，一条都不落");
    }

    @Test
    void inactiveOperatorAlsoFailsOperatorMatch() throws Exception {
        String body = batchJson(List.of(row("OP-003", "CERT-I1", "2025-01-01", "2026-01-01")));
        MvcResult result = submitBatchExpect422(body);
        assertEquals("OPERATOR_NOT_FOUND", rejectedDetail(result).get("conflictType").asText());
    }

    @Test
    void reversedDatesRejectedWithRowNumber() throws Exception {
        String body = batchJson(List.of(
                row("OP-001", "CERT-A1", "2025-01-01", "2026-01-01"),
                row("OP-002", "CERT-B1", "2027-01-01", "2026-01-01")));
        MvcResult result = submitBatchExpect422(body);
        JsonNode detail = rejectedDetail(result);
        assertEquals(2, detail.get("rowNumber").asInt());
        assertEquals("DATE_ORDER", detail.get("conflictType").asText());
        assertEquals(0, certificateRepository.count());
    }

    @Test
    void malformedDateRejected() throws Exception {
        String body = batchJson(List.of(row("OP-001", "CERT-A1", "2025/01/01", "2026-01-01")));
        MvcResult result = submitBatchExpect422(body);
        assertEquals("DATE_FORMAT", rejectedDetail(result).get("conflictType").asText());
    }

    @Test
    void duplicateCertNoAgainstDbReportsCurrentHolder() throws Exception {
        seedExistingCert("OP-001", "CERT-USED", "2024-01-01", "2025-01-01");
        String body = batchJson(List.of(row("OP-002", "CERT-USED", "2025-01-01", "2026-01-01")));
        MvcResult result = submitBatchExpect422(body);
        JsonNode detail = rejectedDetail(result);
        assertEquals("CERT_NO_DUPLICATE_IN_DB", detail.get("conflictType").asText());
        assertEquals("OP-001", detail.get("currentHolderCode").asText(), "要写出目前是谁的工号在用");
        assertEquals("放飞员·李帆", detail.get("currentHolderName").asText(), "要写出目前是谁在用");
        // 库里仍只有种子那一条
        assertEquals(1, certificateRepository.count());
    }

    @Test
    void duplicateCertNoWithinBatchRejected() throws Exception {
        String body = batchJson(List.of(
                row("OP-001", "DUP", "2025-01-01", "2026-01-01"),
                row("OP-002", "DUP", "2025-02-01", "2027-02-01")));
        MvcResult result = submitBatchExpect422(body);
        JsonNode detail = rejectedDetail(result);
        assertEquals(2, detail.get("rowNumber").asInt());
        assertEquals("CERT_NO_DUPLICATE_IN_BATCH", detail.get("conflictType").asText());
        assertEquals(0, certificateRepository.count(), "撞号整批回滚");
    }

    // ---------- 换证到期日次序：批量与单条同一套 ----------

    @Test
    void batchRenewalMustStrictlyExtendLatestExpire() throws Exception {
        seedExistingCert("OP-001", "OLD", "2024-01-01", "2025-12-31");
        // 到期日只到 2025-12-31（等于现有最晚），不严格晚于 -> 换证次序失败
        String body = batchJson(List.of(row("OP-001", "NEW", "2025-06-01", "2025-12-31")));
        MvcResult result = submitBatchExpect422(body);
        JsonNode detail = rejectedDetail(result);
        assertEquals("RENEWAL_ORDER", detail.get("conflictType").asText());
        assertEquals(1, detail.get("rowNumber").asInt());
        assertEquals(1, certificateRepository.count(), "换证失败不得半写入");
        assertTrue(certificateRepository.findByCertificateNo("NEW").isEmpty());
    }

    @Test
    void validRenewalStrictlyLaterSucceeds() throws Exception {
        seedExistingCert("OP-001", "OLD", "2024-01-01", "2025-12-31");
        String body = batchJson(List.of(row("OP-001", "NEW", "2025-06-01", "2026-01-01")));
        mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(batchJson(
                                List.of(row("OP-001", "NEW", "2025-06-01", "2026-01-01")))))
                .andExpect(status().isOk());
        assertEquals(2, certificateRepository.count());
    }

    @Test
    void chainedRenewalWithinSameBatchMustAdvanceExpire() throws Exception {
        seedExistingCert("OP-001", "OLD", "2024-01-01", "2025-12-31");
        // 同一人本批两次换证：第 3 条相对第 2 条没有严格后延 -> 卡在第 3 条
        String body = batchJson(List.of(
                row("OP-001", "N1", "2025-06-01", "2026-06-30"),
                row("OP-002", "B1", "2025-01-01", "2026-01-01"),
                row("OP-001", "N2", "2026-01-01", "2026-06-30")));
        MvcResult result = submitBatchExpect422(body);
        assertEquals(3, rejectedDetail(result).get("rowNumber").asInt());
        assertEquals("RENEWAL_ORDER", rejectedDetail(result).get("conflictType").asText());
        assertEquals(1, certificateRepository.count(), "整批回滚，只剩种子证");
    }

    @Test
    void firstCertForPersonHasNoRenewalOrderConstraint() throws Exception {
        // OP-002 无证，任意合理起止日都可首录
        String body = batchJson(List.of(row("OP-002", "FIRST", "2020-01-01", "2020-02-01")));
        mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        assertEquals(1, certificateRepository.count());
    }

    @Test
    void singleRenewUsesSameOrderingAndIsRejected() throws Exception {
        seedExistingCert("OP-001", "OLD", "2024-01-01", "2025-12-31");
        String body = objectMapper.writeValueAsString(
                row("OP-001", "NEW", "2025-06-01", "2025-12-31"));
        MvcResult result = mockMvc.perform(post("/api/certificates/renew")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
        assertEquals("RENEWAL_ORDER", rejectedDetail(result).get("conflictType").asText(),
                "单条换证不能绕过到期日次序");
        assertEquals(1, certificateRepository.count());
    }

    // ---------- 两人同时交同一批：只许一批成功 ----------

    @Test
    void preHeldLockRejectsBatchWith409AndWritesNothing() throws Exception {
        // 模拟「已经有调度在写台账」：预置一把未释放的锁
        redis.opsForValue().set(LOCK_KEY, "DISP-999:" + UUID.randomUUID(), Duration.ofSeconds(30));
        String body = batchJson(List.of(row("OP-001", "LOCKED", "2025-01-01", "2026-01-01")));
        MvcResult result = mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        assertTrue(json.get("message").asText().contains("已经有调度在写台账"));
        assertEquals(0, certificateRepository.count(), "拿锁失败者不得写入");
    }

    @Test
    void concurrentSubmissionsOnlyOneSucceeds() throws Exception {
        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger locked = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    // 每个线程都给同一个新持证人换同一张证号，制造对同一资源的并发写
                    List<Map<String, String>> rows =
                            List.of(row("OP-002", "RACE-ONLY", "2025-01-01", "2026-01-01"));
                    MvcResult r = mockMvc.perform(post("/api/certificates/batch")
                                    .header("X-Operator-Code", DISP_CODE)
                                    .header("X-Operator-Password", DISP_PWD)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(Map.of("rows", rows))))
                            .andReturn();
                    int s = r.getResponse().getStatus();
                    if (s == 200) ok.incrementAndGet();
                    else if (s == 409) locked.incrementAndGet();
                    else other.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                }
            }));
        }
        ready.await();
        start.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        assertEquals(0, other.get(), "并发下不应出现 409/200 以外的结果");
        assertEquals(1, ok.get(), "只许一批成功");
        assertTrue(locked.get() >= 1, "其余并发批应被告知有人在写台账");
        assertEquals(1, certificateRepository.count(), "台账里只能有一条 RACE-ONLY");
    }

    // ---------- helpers ----------

    private void seedExistingCert(String operatorCode, String no, String issue, String expire) {
        Operator op = operatorRepository.findByOperatorCode(operatorCode).orElseThrow();
        certificateRepository.save(com.example.balloon.entity.ReleaseCertificate.builder()
                .certificateNo(no)
                .operatorCode(op.getOperatorCode())
                .operatorName(op.getOperatorName())
                .issueDate(java.time.LocalDate.parse(issue))
                .expireDate(java.time.LocalDate.parse(expire))
                .batchNo("SEED")
                .createdOperatorCode(DISP_CODE)
                .build());
    }

    private MvcResult submitBatchExpect422(String body) throws Exception {
        return mockMvc.perform(post("/api/certificates/batch")
                        .header("X-Operator-Code", DISP_CODE)
                        .header("X-Operator-Password", DISP_PWD)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();
    }

    private JsonNode rejectedDetail(MvcResult result) throws Exception {
        // MockMvc 不强制响应字符集，getContentAsString 会按 ISO-8859-1 解码中文；
        // 按字节以 UTF-8 解析，避免对中文断言产生乱码
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return root.get("detail");
    }
}
