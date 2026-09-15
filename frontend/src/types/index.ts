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
