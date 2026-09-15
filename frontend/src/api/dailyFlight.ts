import api from './axios'
import type { DailyFlightSummary, FlightRecordRequest } from '@/types'

/** 按日期查询各航线飞行合计（没记过的航线也返回0趟次0时长） */
export const getDailyFlightsByDate = (date: string) =>
  api.get<DailyFlightSummary[]>('/daily-flights', { params: { date } })

/** 查询单条航线某天的飞行合计 */
export const getDailyFlight = (routeId: number, date: string) =>
  api.get<DailyFlightSummary>(`/daily-flights/route/${routeId}`, { params: { date } })

/**
 * 登记一笔飞行记录。
 * 同一天同一条航线再记一笔，趟次与时长叠加到原合计行，不另开新行。
 */
export const recordDailyFlight = (data: FlightRecordRequest) =>
  api.post<DailyFlightSummary>('/daily-flights', data)
