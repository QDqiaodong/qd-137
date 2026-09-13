import api from './axios'
import type { WindMatchLog } from '@/types'

export const getLogsByRoute = (routeId: number) =>
  api.get<WindMatchLog[]>(`/logs/route/${routeId}`)
