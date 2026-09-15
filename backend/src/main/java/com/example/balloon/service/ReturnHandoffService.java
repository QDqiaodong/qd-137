package com.example.balloon.service;

import com.example.balloon.dto.AddReturnItemRequest;
import com.example.balloon.dto.DutyShiftDTO;
import com.example.balloon.dto.HandoffStatusDTO;
import com.example.balloon.dto.ReturnItemDTO;
import com.example.balloon.entity.Bracket;
import com.example.balloon.entity.BracketReturnItem;
import com.example.balloon.entity.DutyShift;
import com.example.balloon.entity.Operator;
import com.example.balloon.exception.AccessDeniedException;
import com.example.balloon.repository.BracketRepository;
import com.example.balloon.repository.BracketReturnItemRepository;
import com.example.balloon.repository.DutyShiftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 地勤支架归位交接。
 *
 * 规则：
 * 1. 全班组同时只有一个进行中（ACTIVE）的班组；下班交接后置为 CLOSED。
 * 2. 交接间隙打开归位页，先看到上一班遗留、还停在场地（ON_SITE）的支架；
 *    只要还有未收回的，就不能开始本班。
 * 3. 交接间隙可继续替上一班把遗留支架标记收回；全部收回后页面提示本班可以开始作业。
 * 4. 只有本班进行中才能往清单里登记当天动过的支架；同一支架同一班不重复登记。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnHandoffService {

    private final DutyShiftRepository shiftRepository;
    private final BracketReturnItemRepository itemRepository;
    private final BracketRepository bracketRepository;

    /**
     * 归位页状态：本班进行中 -> 本班清单；交接间隙 -> 上一班遗留未收回清单。
     */
    @Transactional(readOnly = true)
    public HandoffStatusDTO getHandoffStatus() {
        Optional<DutyShift> active = shiftRepository
                .findFirstByShiftStatusOrderByStartedAtDesc(DutyShift.STATUS_ACTIVE);

        if (active.isPresent()) {
            DutyShift shift = active.get();
            return HandoffStatusDTO.builder()
                    .activeShift(toShiftDTO(shift))
                    .pendingShift(null)
                    .pendingItems(List.of())
                    .canStartShift(false)
                    .message("本班进行中，请在下班前登记当天动过的支架并标明归位状态")
                    .build();
        }

        // 交接间隙：找上一班（最近一个班组）
        Optional<DutyShift> last = shiftRepository.findFirstByOrderByStartedAtDesc();
        if (last.isEmpty()) {
            return HandoffStatusDTO.builder()
                    .activeShift(null)
                    .pendingShift(null)
                    .pendingItems(List.of())
                    .canStartShift(true)
                    .message("暂无班组记录，本班可以开始作业")
                    .build();
        }

        DutyShift pendingShift = last.get();
        List<BracketReturnItem> onSiteItems = itemRepository
                .findByShiftIdOrderByCreatedAtAsc(pendingShift.getId()).stream()
                .filter(i -> BracketReturnItem.STATUS_ON_SITE.equals(i.getReturnStatus()))
                .collect(Collectors.toList());

        boolean allReturned = onSiteItems.isEmpty();
        String message = allReturned
                ? "上一班支架已全部收回，本班可以开始作业"
                : "上一班还有 " + onSiteItems.size() + " 个支架未收回，全部收回后才能开始本班";

        return HandoffStatusDTO.builder()
                .activeShift(null)
                .pendingShift(toShiftDTO(pendingShift))
                .pendingItems(onSiteItems.stream().map(this::toItemDTO).collect(Collectors.toList()))
                .canStartShift(allReturned)
                .message(message)
                .build();
    }

    /**
     * 开始本班：仅交接间隙、且上一班没有遗留 ON_SITE 支架时允许。
     */
    @Transactional
    public DutyShiftDTO startShift(Operator operator) {
        Optional<DutyShift> active = shiftRepository
                .findFirstByShiftStatusOrderByStartedAtDesc(DutyShift.STATUS_ACTIVE);
        if (active.isPresent()) {
            throw new AccessDeniedException("本班已在进行中（" + active.get().getShiftCode() + "），不能重复开始");
        }

        shiftRepository.findFirstByOrderByStartedAtDesc().ifPresent(last -> {
            long remaining = itemRepository.countByShiftIdAndReturnStatus(
                    last.getId(), BracketReturnItem.STATUS_ON_SITE);
            if (remaining > 0) {
                throw new AccessDeniedException(
                        "上一班还有 " + remaining + " 个支架停在场地未收回，全部收回后才能开始本班");
            }
        });

        DutyShift shift = DutyShift.builder()
                .shiftCode(generateShiftCode())
                .shiftStatus(DutyShift.STATUS_ACTIVE)
                .startOperatorCode(operator.getOperatorCode())
                .build();
        DutyShift saved = shiftRepository.save(shift);
        log.info("Shift {} started by {}", saved.getShiftCode(), operator.getOperatorCode());
        return toShiftDTO(saved);
    }

    /**
     * 登记一个本班当天动过的支架，初始状态为「还停在场地」。
     */
    @Transactional
    public ReturnItemDTO addItem(Operator operator, AddReturnItemRequest request) {
        DutyShift shift = requireActiveShift();

        Bracket bracket = bracketRepository.findById(request.getBracketId())
                .filter(b -> "ACTIVE".equals(b.getStatus()))
                .orElseThrow(() -> new EntityNotFoundException("支架不存在或已停用: " + request.getBracketId()));

        Optional<BracketReturnItem> existing = itemRepository
                .findByShiftIdAndBracketId(shift.getId(), bracket.getId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("该支架本班已登记过，不能重复登记");
        }

        BracketReturnItem item = BracketReturnItem.builder()
                .shiftId(shift.getId())
                .bracketId(bracket.getId())
                .bracketCode(bracket.getBracketCode())
                .bracketName(bracket.getBracketName())
                .returnStatus(BracketReturnItem.STATUS_ON_SITE)
                .createdOperatorCode(operator.getOperatorCode())
                .build();
        BracketReturnItem saved = itemRepository.save(item);
        log.info("Bracket {} added to shift {} by {}",
                bracket.getBracketCode(), shift.getShiftCode(), operator.getOperatorCode());
        return toItemDTO(saved);
    }

    /**
     * 标记某条清单的归位状态：RETURNED=已收回停放区 / ON_SITE=还停在场地。
     * 本班进行中可改本班条目；交接间隙可改最近一班（即上一班）的遗留条目。
     */
    @Transactional
    public ReturnItemDTO markStatus(Operator operator, Long itemId, String returnStatus) {
        if (!BracketReturnItem.STATUS_RETURNED.equals(returnStatus)
                && !BracketReturnItem.STATUS_ON_SITE.equals(returnStatus)) {
            throw new IllegalArgumentException("归位状态只能是 RETURNED 或 ON_SITE");
        }

        BracketReturnItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("清单项不存在: " + itemId));

        DutyShift shift = shiftRepository.findById(item.getShiftId())
                .orElseThrow(() -> new EntityNotFoundException("班组不存在: " + item.getShiftId()));
        if (DutyShift.STATUS_ACTIVE.equals(shift.getShiftStatus())) {
            // 本班条目，直接可改
        } else if (isLatestShift(shift)) {
            // 交接间隙，上一班的遗留条目可继续标记收回
        } else {
            throw new AccessDeniedException("该班组已完成交接且不是最近一班，清单不能再改动");
        }

        item.setReturnStatus(returnStatus);
        if (BracketReturnItem.STATUS_RETURNED.equals(returnStatus)) {
            item.setReturnedOperatorCode(operator.getOperatorCode());
            item.setReturnedAt(java.time.LocalDateTime.now());
        } else {
            item.setReturnedOperatorCode(null);
            item.setReturnedAt(null);
        }
        BracketReturnItem saved = itemRepository.save(item);
        log.info("Item {} marked {} by {}", itemId, returnStatus, operator.getOperatorCode());
        return toItemDTO(saved);
    }

    /**
     * 下班交接：把当前班组置为 CLOSED。
     * 仍有支架停在场地时允许交接，这些支架会作为遗留项交给下一班先收回。
     */
    @Transactional
    public DutyShiftDTO closeShift(Operator operator) {
        DutyShift shift = requireActiveShift();
        shift.setShiftStatus(DutyShift.STATUS_CLOSED);
        shift.setCloseOperatorCode(operator.getOperatorCode());
        shift.setClosedAt(java.time.LocalDateTime.now());
        DutyShift saved = shiftRepository.save(shift);

        long onSite = itemRepository.countByShiftIdAndReturnStatus(
                shift.getId(), BracketReturnItem.STATUS_ON_SITE);
        log.info("Shift {} closed by {} with {} bracket(s) left on site",
                saved.getShiftCode(), operator.getOperatorCode(), onSite);
        return toShiftDTO(saved);
    }

    /**
     * 查看某个班组的完整归位清单（已收回 + 未收回）。
     */
    @Transactional(readOnly = true)
    public List<ReturnItemDTO> listItems(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new EntityNotFoundException("班组不存在: " + shiftId);
        }
        return itemRepository.findByShiftIdOrderByCreatedAtAsc(shiftId).stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
    }

    private DutyShift requireActiveShift() {
        return shiftRepository
                .findFirstByShiftStatusOrderByStartedAtDesc(DutyShift.STATUS_ACTIVE)
                .orElseThrow(() -> new AccessDeniedException(
                        "本班尚未开始：请先确认上一班支架全部收回，再点击「开始本班」"));
    }

    private boolean isLatestShift(DutyShift shift) {
        return shiftRepository.findFirstByOrderByStartedAtDesc()
                .map(latest -> latest.getId().equals(shift.getId()))
                .orElse(false);
    }

    /**
     * 班组编号：SHIFT-yyyyMMdd-当日序号（三位）。
     */
    private String generateShiftCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String prefix = "SHIFT-" + datePart + "-";
        long seq = shiftRepository.findAll().stream()
                .map(DutyShift::getShiftCode)
                .filter(code -> code.startsWith(prefix))
                .mapToLong(code -> {
                    try {
                        return Long.parseLong(code.substring(prefix.length()));
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max().orElse(0) + 1;
        return prefix + String.format("%03d", seq);
    }

    private DutyShiftDTO toShiftDTO(DutyShift shift) {
        List<BracketReturnItem> items = itemRepository.findByShiftIdOrderByCreatedAtAsc(shift.getId());
        long onSite = items.stream()
                .filter(i -> BracketReturnItem.STATUS_ON_SITE.equals(i.getReturnStatus()))
                .count();
        return DutyShiftDTO.builder()
                .id(shift.getId())
                .shiftCode(shift.getShiftCode())
                .shiftStatus(shift.getShiftStatus())
                .startOperatorCode(shift.getStartOperatorCode())
                .closeOperatorCode(shift.getCloseOperatorCode())
                .startedAt(shift.getStartedAt())
                .closedAt(shift.getClosedAt())
                .totalItems(items.size())
                .onSiteItems(onSite)
                .build();
    }

    private ReturnItemDTO toItemDTO(BracketReturnItem entity) {
        return ReturnItemDTO.builder()
                .id(entity.getId())
                .shiftId(entity.getShiftId())
                .bracketId(entity.getBracketId())
                .bracketCode(entity.getBracketCode())
                .bracketName(entity.getBracketName())
                .returnStatus(entity.getReturnStatus())
                .createdOperatorCode(entity.getCreatedOperatorCode())
                .returnedOperatorCode(entity.getReturnedOperatorCode())
                .createdAt(entity.getCreatedAt())
                .returnedAt(entity.getReturnedAt())
                .build();
    }
}
