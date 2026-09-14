import { reactive } from 'vue'
import type { OperatorInfo } from '@/types'

const STORAGE_KEY = 'binding-identity'

interface StoredIdentity {
  operator: OperatorInfo
  password: string
}

const load = (): StoredIdentity | null => {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as StoredIdentity) : null
  } catch {
    return null
  }
}

const restored = load()

/**
 * 当前已完成身份核对的人员。口令仅保存在会话内，
 * 用于后续请求头携带身份，后端每次请求都会重新核对。
 */
export const identity = reactive<{ operator: OperatorInfo | null; password: string }>({
  operator: restored?.operator ?? null,
  password: restored?.password ?? ''
})

export const setIdentity = (operator: OperatorInfo, password: string) => {
  identity.operator = operator
  identity.password = password
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ operator, password }))
}

export const clearIdentity = () => {
  identity.operator = null
  identity.password = ''
  sessionStorage.removeItem(STORAGE_KEY)
}

export const isDispatcher = () => identity.operator?.role === 'DISPATCHER'
