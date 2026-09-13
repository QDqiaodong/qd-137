import api from './axios'
import type { Route, Bracket, WindMatchResult } from '@/types'

export const getRoutes = () => api.get<Route[]>('/routes')

export const getRouteById = (id: number) => api.get<Route>(`/routes/${id}`)

export const createRoute = (data: Omit<Route, 'id'>) => api.post<Route>('/routes', data)

export const updateRoute = (id: number, data: Partial<Route>) => api.put<Route>(`/routes/${id}`, data)

export const deleteRoute = (id: number) => api.delete(`/routes/${id}`)

export const getRouteGroups = () => api.get<string[]>('/routes/groups')

export const getRoutesByGroup = (groupName: string) => api.get<Route[]>(`/routes/group/${groupName}`)

export const getSuitableBracketsForRoute = (routeId: number) => 
  api.get<Bracket[]>(`/routes/${routeId}/suitable-brackets`)

export const rematchBracketsForRoute = (routeId: number) =>
  api.get<WindMatchResult>(`/routes/${routeId}/match-result`)
