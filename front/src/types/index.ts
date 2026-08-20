/** 用户信息 */
export interface User {
  id: number
  username: string
  email: string
  phone: string
  nickname: string
  avatar: string
  status: number
  createTime: string
  updateTime: string
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 注册请求 */
export interface RegisterRequest {
  username: string
  password: string
  email: string
  phone: string
}

/** 用户更新请求 */
export interface UserUpdateRequest {
  username: string
  email: string
  phone: string
  nickname: string
  avatar: string
}

/** 密码修改请求 */
export interface PasswordUpdateRequest {
  oldPassword: string
  newPassword: string
}

/** API 统一响应 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 登录响应数据 */
export interface LoginResponse {
  token: string
  userInfo: User
}

/** 通道配置 */
export interface Channel {
  id: number
  name: string
  code: string
  host: string
  port: number
  spId: string
  sharedSecret: string
  version: number
  heartbeatInterval: number
  reconnectInterval: number
  maxReconnectInterval: number
  connectTimeout: number
  maxConcurrent: number
  costPrice: number | null
  status: number
  description: string
  createTime: string
  updateTime: string
}

/** 通道列表响应 */
export interface ChannelListData {
  list: Channel[]
  total: number
  page: number
  size: number
}

/** 下行短信记录 */
export interface SmsDown {
  id: number
  msgId: string
  srcId: string
  destTerminalId: string
  msgContent: string
  msgFmt: number
  serviceId: string
  channelCode: string
  status: number
  statusReport: string
  errorMsg: string
  signature: string
  carrier: string
  createTime: string
  updateTime: string
  statusReportTime: string
}

/** 下行短信详情 */
export interface SmsDownDetail {
  id: number
  msgId: string
  serverMsgId: string
  spId: string
  srcId: string
  destTerminalId: string
  msgContent: string
  msgFmt: number
  serviceId: string
  channelCode: string
  status: number
  statusReport: string
  errorMsg: string
  signature: string
  carrier: string
  fee: number
  createTime: string
  updateTime: string
  statusReportTime: string
  channelName: string
  channelHost: string
  channelPort: number
  channelStatus: number
}

/** 上行短信记录 */
export interface SmsUp {
  id: number
  msgId: string
  spId: string
  srcTerminalId: string
  destId: string
  msgContent: string
  msgFmt: number
  serviceId: string
  isReport: number
  reportStat: string
  carrier: string
  channelCode: string
  createTime: string
  /** 关联下行短信内容 */
  downMsgContent: string
}

/** 上行短信详情（含关联下行与通道信息） */
export interface SmsUpDetail {
  id: number
  msgId: string
  spId: string
  srcTerminalId: string
  destId: string
  msgContent: string
  msgFmt: number
  serviceId: string
  isReport: number
  reportStat: string
  carrier: string
  channelCode: string
  createTime: string
  downMsgContent: string
  downCreateTime: string
  channelName: string
  channelHost: string
  channelPort: number
  channelStatus: number
}

/** 短信列表响应 */
export interface SmsListData<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 下游客户 */
export interface Sp {
  id: number
  spId: string
  spSecret: string
  name: string
  status: number
  description: string
  balance: number
  unitPrice: number
  rateLimit: number
  createTime: string
  updateTime: string
  channelCodes: string[]
}

/** 客户列表响应 */
export interface SpListData {
  list: Sp[]
  total: number
  page: number
  size: number
}

/** 客户余额流水 */
export interface SpTransaction {
  id: number
  spId: string
  type: string
  amount: number
  balanceAfter: number
  refMsgId: string | null
  remark: string | null
  createTime: string
}

/** 流水列表响应 */
export interface SpTransactionListData {
  list: SpTransaction[]
  total: number
  page: number
  size: number
}

/** 短信签名 */
export interface Signature {
  id: number
  content: string
  spId: string | null
  status: number
  remark: string | null
  createTime: string
  updateTime: string
}

/** 短信模板 */
export interface Template {
  id: number
  name: string
  content: string
  signatureId: number | null
  signatureContent: string | null
  status: number
  remark: string | null
  createTime: string
  updateTime: string
}

/** 敏感词 */
export interface SensitiveWord {
  id: number
  word: string
  status: number
  createTime: string
}

/** 审计日志 */
export interface AuditLog {
  id: number
  adminId: number | null
  username: string | null
  module: string
  action: string
  target: string | null
  detail: string | null
  ip: string | null
  createTime: string
}

/** 通用分页列表响应 */
export interface PageData<T> {
  list: T[]
  total: number
  page: number
  size: number
}

/** 看板概览 */
export interface StatsOverview {
  spTotal: number
  spEnabled: number
  channelTotal: number
  channelEnabled: number
  channelOnline: number
  adminTotal: number
  todayTotal: number
  todaySuccess: number
  todayFail: number
  todayActiveSp: number
}

/** 按天趋势点 */
export interface TrendPoint {
  date: string
  total: number
  success: number
}

/** 通道维度统计 */
export interface ChannelStat {
  channelCode: string
  total: number
  success: number
  fail: number
  successRate: number
}

/** 客户维度统计 */
export interface SpStat {
  spId: string
  total: number
  success: number
  fee: number
  successRate: number
}

/** 退订黑名单记录 */
export interface Blacklist {
  id: number
  channelCode: string
  phone: string
  keyword: string
  sourceMoId: string
  expireTime: string
  createTime: string
}

/** 黑名单列表响应 */
export interface BlacklistListData {
  list: Blacklist[]
  total: number
  page: number
  size: number
}
