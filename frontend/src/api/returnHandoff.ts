import api from './axios'
import type { DutyShift, HandoffStatus, ReturnItem } from '@/types'

/** 打开归位页：本班进行中返回本班；交接间隙返回上一班遗留未收回清单 */
export const getHandoffStatus = () =>
  api.get<HandoffStatus>('/returns/handoff')

/** 开始本班（上一班仍有未收回支架时后端返回 403） */
export const startShift = () =>
  api.post<DutyShift>('/returns/shifts/start')

/** 下班交接（本班置为 CLOSED，未收回支架留给下一班） */
export const closeShift = () =>
  api.post<DutyShift>('/returns/shifts/close')

/** 查看某个班组的完整归位清单 */
export const getShiftItems = (shiftId: number) =>
  api.get<ReturnItem[]>(`/returns/shifts/${shiftId}/items`)

/** 本班进行中：登记一个当天动过的支架（初始为还停在场地） */
export const addReturnItem = (bracketId: number) =>
  api.post<ReturnItem>('/returns/items', { bracketId })

/** 标记归位状态：RETURNED=已收回停放区 / ON_SITE=还停在场地 */
export const markItemStatus = (itemId: number, returnStatus: string) =>
  api.put<ReturnItem>(`/returns/items/${itemId}/status`, { returnStatus })
