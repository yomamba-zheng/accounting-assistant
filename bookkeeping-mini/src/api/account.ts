import { request } from './request';
import { Account, AccountRecord } from '../types';

// 获取账户列表
export const getAccountList = (userId?: number) =>
  request<Account[]>({
    url: '/api/accounts',
    method: 'GET',
    data: { userId }
  });

// 获取默认账户
export const getDefaultAccount = (userId?: number) =>
  request<Account>({
    url: '/api/accounts/default',
    method: 'GET',
    data: { userId }
  });

// 创建账户
export interface CreateAccountParams {
  userId?: number;
  name: string;
  type?: string;
  balance?: number;
  icon?: string;
  color?: string;
}

export const createAccount = (data: CreateAccountParams) =>
  request<Account>({
    url: '/api/accounts',
    method: 'POST',
    data
  });

// 更新账户
export interface UpdateAccountParams {
  userId?: number;
  name?: string;
  icon?: string;
  color?: string;
  isDefault?: boolean;
}

export const updateAccount = (id: number, data: UpdateAccountParams) =>
  request<Account>({
    url: `/api/accounts/${id}`,
    method: 'PUT',
    data
  });

// 删除账户
export const deleteAccount = (id: number, userId?: number) =>
  request<void>({
    url: `/api/accounts/${id}`,
    method: 'DELETE',
    data: { userId }
  });

// 获取账户变动记录
export const getAccountRecords = (id: number, userId?: number, limit?: number) =>
  request<AccountRecord[]>({
    url: `/api/accounts/${id}/records`,
    method: 'GET',
    data: { userId, limit }
  });
