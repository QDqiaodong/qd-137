import api from './axios'
import type { Binding, WindMatchResult } from '@/types'

export const createBinding = (routeId: number, bracketId: number) =>
  api.post<WindMatchResult>('/bindings', { routeId, bracketId })

export const deleteBinding = (routeId: number, bracketId: number) =>
  api.delete(`/bindings/route/${routeId}/bracket/${bracketId}`)

export const getBindingsByRoute = (routeId: number) =>
  api.get<Binding[]>(`/bindings/route/${routeId}`)

export const getBindingsByBracket = (bracketId: number) =>
  api.get<Binding[]>(`/bindings/bracket/${bracketId}`)

export const countBindings = (routeId: number) =>
  api.get<{ count: number }>(`/bindings/route/${routeId}/count`)

export const checkMatch = (routeId: number, bracketId: number) =>
  api.get<WindMatchResult>(`/bindings/check-match`, { params: { routeId, bracketId } })
