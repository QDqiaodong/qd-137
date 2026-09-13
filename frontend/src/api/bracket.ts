import api from './axios'
import type { Bracket } from '@/types'

export const getBrackets = () => api.get<Bracket[]>('/brackets')

export const getBracketById = (id: number) => api.get<Bracket>(`/brackets/${id}`)

export const createBracket = (data: Omit<Bracket, 'id'>) => api.post<Bracket>('/brackets', data)

export const updateBracket = (id: number, data: Partial<Bracket>) => api.put<Bracket>(`/brackets/${id}`, data)

export const deleteBracket = (id: number) => api.delete(`/brackets/${id}`)

export const getBracketTypes = () => api.get<string[]>('/brackets/types')

export const getBracketsByType = (type: string) => api.get<Bracket[]>(`/brackets/type/${type}`)

export const searchBrackets = (minWindSpeed: number, maxWindSpeed: number) => 
  api.get<Bracket[]>('/brackets/search', { params: { minWindSpeed, maxWindSpeed } })

export const getSuitableBrackets = (minWindSpeed: number, maxWindSpeed: number) =>
  api.get<Bracket[]>('/brackets/suitable', { params: { minWindSpeed, maxWindSpeed } })
