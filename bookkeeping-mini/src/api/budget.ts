import { request } from './request';
import { Budget } from '../types';

// 获取预算列表
export const getBudgetList = (userId?: number) =>
  request<Budget[]>({
    url: '/api/budgets',
    method: 'GET',
    data: { userId }
  });

// 设置预算
export interface SetBudgetParams {
  userId?: number;
  amount: number;
  categoryId?: number;
  periodType?: string;
}

export const setBudget = (data: SetBudgetParams) =>
  request<Budget>({
    url: '/api/budgets',
    method: 'POST',
    data
  });

// 删除预算
export const deleteBudget = (id: number, userId?: number) =>
  request<void>({
    url: `/api/budgets/${id}`,
    method: 'DELETE',
    data: { userId }
  });

// 检查超支
export const checkOverBudget = (userId?: number) =>
  request<Budget[]>({
    url: '/api/budgets/check-overbudget',
    method: 'GET',
    data: { userId }
  });
