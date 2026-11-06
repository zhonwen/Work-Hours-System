import request from './request'
import type { ApiResponse } from '@/types'

// 用户登录（password 为 RSA 密文：明文密码|cid）
export interface LoginReq {
  username: string
  password: string
  cid: string
  captchaCode: string
}

export interface LoginResp {
  token: string
  userInfo: {
    id: number
    username: string
    nickname?: string
  }
}

export interface CaptchaResp {
  cid: string
  publicKey: string
  imageBase64: string
}

export const login = (data: LoginReq): Promise<ApiResponse<LoginResp>> => {
  return request.post('/users/login', data)
}

/** 获取登录/注册等场景验证码（含 RSA 公钥） */
export const getCaptcha = (): Promise<CaptchaResp> => {
  return request.get('/captcha')
}

// 用户注册
export interface RegisterReq {
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  cid: string
  captchaCode: string
}

export const register = (data: RegisterReq): Promise<ApiResponse<string>> => {
  return request.post('/users/register', data)
}

/** 修改密码（old/new 均为 RSA 密文） */
export interface PasswordChangeReq {
  oldPassword: string
  newPassword: string
  cid: string
  captchaCode: string
}

export const changePassword = (data: PasswordChangeReq) => {
  return request.put('/users/password', data)
}

// 获取用户信息
export const getUserInfo = (): Promise<ApiResponse<unknown>> => {
  return request.get('/users/info')
}

/**
 * 获取当前用户权限码列表
 */
export const getUserPermissions = (): Promise<string[]> => {
  return request.get('/auth/permissions')
}

/**
 * 获取当前用户角色编码列表
 */
export const getUserRoles = (): Promise<string[]> => {
  return request.get('/auth/roles')
}
