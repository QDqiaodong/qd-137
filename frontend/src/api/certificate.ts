import api from './axios'
import type {
  CertificateRow,
  CertificateBatchResult,
  OperatorInfo,
  ReleaseCertificate
} from '@/types'

/** 台账全量（调度可写、放飞员只读，两者都能查） */
export const listCertificates = () =>
  api.get<ReleaseCertificate[]>('/certificates')

/** 在册在职人员清单（单条换证选人、补录核对工号用） */
export const listCertificateOperators = () =>
  api.get<OperatorInfo[]>('/certificates/operators')

/**
 * 批量补录/换证：整批原子。
 * 任一条不合格时后端返回 422，error.response.data.detail 里带：
 * 卡在第几条（rowNumber 从 1 开始）、原因 reason、撞号时当前占用人。
 * 两人同时交批时失败者返回 409（已有调度在写台账）。
 */
export const submitCertificateBatch = (rows: CertificateRow[]) =>
  api.post<CertificateBatchResult>('/certificates/batch', { rows })

/**
 * 单条换证：与批量同一套到期日次序与整批校验，不能只拦批量。
 */
export const renewCertificate = (row: CertificateRow) =>
  api.post<CertificateBatchResult>('/certificates/renew', row)
