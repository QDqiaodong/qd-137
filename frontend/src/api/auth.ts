import api from './axios'
import type { OperatorInfo } from '@/types'

export const verifyIdentity = (operatorCode: string, password: string) =>
  api.post<OperatorInfo>('/auth/verify', { operatorCode, password })
