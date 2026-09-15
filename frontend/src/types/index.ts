export interface Bracket {
  id: number
  bracketCode: string
  bracketName: string
  maxLoad: number
  minWindSpeed: number
  maxWindSpeed: number
  bracketType: string
  status: string
  description: string
}

export interface Route {
  id: number
  routeCode: string
  routeName: string
  groupName: string
  minWindSpeed: number
  maxWindSpeed: number
  distance: number
  duration: number
  difficultyLevel: string
  status: string
  description: string
}

export interface Binding {
  id: number
  routeId: number
  routeCode: string
  routeName: string
  bracketId: number
  bracketCode: string
  bracketName: string
  maxLoad: number
  bracketMinWind: number
  bracketMaxWind: number
  matchLevel: string
  launchWindSpeed?: number | null
  windStatus?: string
  status: string
}

export interface WindMatchResult {
  matched: boolean
  matchLevel: string
  message: string
  windStatus?: string
  launchWindSpeed?: number | null
  suitableBrackets?: Bracket[]
  unsuitableBrackets?: Bracket[]
}

export interface OperatorInfo {
  operatorCode: string
  operatorName: string
  /** DISPATCHER=调度；LAUNCH_OPERATOR=放飞员 */
  role: string
  /** 放飞员当班航线ID列表；调度为空（不限航线） */
  dutyRouteIds: number[]
}

export interface WindMatchLog {
  id: number
  routeId: number
  routeCode: string
  bracketId: number
  bracketCode: string
  oldMinWind: number
  oldMaxWind: number
  newMinWind: number
  newMaxWind: number
  actionType: string
  matchResult: string
  description: string
  createdAt: string
}

/** 某条航线某一天的飞行合计 */
export interface DailyFlightSummary {
  routeId: number
  routeCode: string
  routeName: string
  flightDate: string
  /** 当天累计趟次；没记过为0 */
  flightCount: number
  /** 当天累计时长（分钟）；没记过为0 */
  totalDuration: number
  /** 当天是否有过登记 */
  recorded: boolean
}

/** 登记一笔飞行记录 */
export interface FlightRecordRequest {
  routeId: number
  flightDate: string
  flightCount: number
  durationMinutes: number
}

/** 归位清单条目 */
export interface ReturnItem {
  id: number
  shiftId: number
  bracketId: number
  bracketCode: string
  bracketName: string
  /** RETURNED=已收回停放区；ON_SITE=还停在场地 */
  returnStatus: string
  createdOperatorCode: string
  returnedOperatorCode?: string | null
  createdAt: string
  returnedAt?: string | null
}

/** 班组（一班） */
export interface DutyShift {
  id: number
  shiftCode: string
  /** ACTIVE=本班进行中；CLOSED=已交接下班 */
  shiftStatus: string
  startOperatorCode: string
  closeOperatorCode?: string | null
  startedAt: string
  closedAt?: string | null
  totalItems: number
  onSiteItems: number
}

/** 归位页交接状态 */
export interface HandoffStatus {
  /** 当前进行中的班组；交接间隙为 null */
  activeShift: DutyShift | null
  /** 交接间隙待收回的上一班；本班进行中为 null */
  pendingShift: DutyShift | null
  /** 交接间隙需要先收回的清单（上一班未收回条目） */
  pendingItems: ReturnItem[]
  /** 是否可以开始本班 */
  canStartShift: boolean
  /** 页面提示语 */
  message: string
}

/** 台账中的一张放飞证 */
export interface ReleaseCertificate {
  id: number
  certificateNo: string
  operatorCode: string
  operatorName: string
  /** yyyy-MM-dd */
  issueDate: string
  /** yyyy-MM-dd */
  expireDate: string
  batchNo: string
  createdOperatorCode: string
  createdAt: string
}

/** 批量补录/换证的一行（字符串接收，后端逐条校验并回带行号） */
export interface CertificateRow {
  operatorCode: string
  certificateNo: string
  issueDate: string
  expireDate: string
}

/** 整批落库结果（成功） */
export interface CertificateBatchResult {
  batchNo: string
  savedCount: number
  message: string
}

/** 整批被拦下的行级说明（后端 422 带回） */
export interface BatchRejectedDetail {
  /** 卡住的行号，从 1 开始 */
  rowNumber: number
  row: CertificateRow
  reason: string
  conflictType: string
  currentHolderName?: string | null
  currentHolderCode?: string | null
}

