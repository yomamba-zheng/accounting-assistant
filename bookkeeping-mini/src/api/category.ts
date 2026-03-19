import { request } from './request';
import { Category, ApiResponse } from '../types';

export const getCategoryList = (type?: number) => 
  request<Category[]>({ url: '/api/bills/categories', method: 'GET', data: { type } });