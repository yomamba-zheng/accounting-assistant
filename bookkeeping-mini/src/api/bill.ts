import { request } from './request';
import { Bill, QuickAddPreview, StatTotal, VisualizationData } from '../types';

// 获取账单列表（分页）
export interface BillListParams {
  page?: number;
  size?: number;
  categoryId?: number;
  startTime?: string;
  endTime?: string;
}

export interface BillListResponse {
  records: Bill[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export const getBillList = (params?: BillListParams) =>
  request<BillListResponse>({ url: '/api/bills', method: 'GET', data: params });

// 获取统计信息（总收入 / 总支出等）
// 注意：如果后端没有这个接口，需要后端添加或者前端自己计算
export const getStatistics = () =>
  request<StatTotal>({ url: '/api/bills/statistics', method: 'GET' });

// 删除账单
export const deleteBill = (id: number) =>
  request<void>({ url: `/api/bills/${id}`, method: 'DELETE' });

// 新增账单
export const addBill = (data: {
  amount: number;
  categoryId: number;
  remark?: string;
  billTime: string;
  type?: 1 | 2; // 1=支出 2=收入
}) =>
  request<void>({ url: '/api/bills', method: 'POST', data });

// 快速记账预览（不保存，只返回解析结果）
export const previewQuickAdd = (text: string, billTime?: string) =>
  request<QuickAddPreview>({
    url: '/api/bills/quick-add/preview',
    method: 'GET',
    data: { text, billTime }
  });

// 快速记账
export const quickAdd = (text: string, userId?: number, billTime?: string, type?: 1 | 2) =>
  request<Bill>({
    url: '/api/bills/quick-add',
    method: 'POST',
    data: { text, userId, billTime, type }
  });

// 获取可视化数据（饼图、柱状图、折线图）
export interface VisualizationParams {
  type?: 'pie' | 'bar' | 'line' | 'all';
  months?: number;
  startTime?: string;
  endTime?: string;
}

export const getVisualizationData = (params?: VisualizationParams) =>
  request<VisualizationData>({
    url: '/api/bills/statistics/visualization',
    method: 'GET',
    data: params
  });