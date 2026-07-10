import request from '../utils/axios'
import type {
  ApiResponse,
  User,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserUpdateRequest,
  PasswordUpdateRequest
} from '../types'

/** 用户登录 */
export function login(data: LoginRequest) {
  return request.post<any, ApiResponse<LoginResponse>>('/user/login', data)
}

/** 用户注册 */
export function register(data: RegisterRequest) {
  return request.post<any, ApiResponse<string>>('/user/register', data)
}

/** 用户登出 */
export function logout() {
  return request.post<any, ApiResponse<string>>('/user/logout')
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request.get<any, ApiResponse<User>>('/user/info')
}

/** 更新用户信息 */
export function updateUser(data: UserUpdateRequest) {
  return request.put<any, ApiResponse<string>>('/user/info', data)
}

/** 修改密码 */
export function updatePassword(data: PasswordUpdateRequest) {
  return request.put<any, ApiResponse<string>>('/user/password', data)
}

/** 获取用户列表 */
export function getUserList(page: number, size: number) {
  return request.get<any, ApiResponse<User[]>>('/user/list', {
    params: { page, size }
  })
}

/** 获取用户总数 */
export function getUserCount() {
  return request.get<any, ApiResponse<number>>('/user/count')
}

/** 禁用用户 */
export function disableUser(userId: number) {
  return request.put<any, ApiResponse<string>>(`/user/${userId}/disable`)
}

/** 启用用户 */
export function enableUser(userId: number) {
  return request.put<any, ApiResponse<string>>(`/user/${userId}/enable`)
}
