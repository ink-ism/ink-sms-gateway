import request from '../utils/axios'
import type { ApiResponse, SmsDown, SmsUp, SmsListData } from '../types'

/** 获取下行短信列表 */
export function getSmsDownList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<SmsListData<SmsDown>>>('/admin/sms/down/list', {
    params: { page, size, keyword }
  })
}

/** 获取上行短信列表 */
export function getSmsUpList(page: number, size: number, keyword?: string) {
  return request.get<any, ApiResponse<SmsListData<SmsUp>>>('/admin/sms/up/list', {
    params: { page, size, keyword }
  })
}
