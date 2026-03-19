import { ApiResponse } from '../types';

// 【重要】这里配置您的后端地址
const BASE_URL = 'http://localhost:8080'; 

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: any;
  header?: any;
}

// 过滤掉 undefined 和 null 值的辅助函数
const filterParams = (data: any): any => {
  if (!data) return {};
  const filtered: any = {};
  for (const key in data) {
    if (data[key] !== undefined && data[key] !== null && data[key] !== '') {
      filtered[key] = data[key];
    }
  }
  return filtered;
};

export const request = <T>(options: RequestOptions): Promise<ApiResponse<T>> => {
  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: filterParams(options.data),
      header: {
        'Content-Type': 'application/json',
        // 如果有 Token，可以在这里添加: 'Authorization': 'Bearer ' + token
        ...options.header,
      },
      success: (res) => {
        // 状态码 200 通常表示 HTTP 成功，具体业务逻辑看 res.data.code
        if (res.statusCode === 200) {
          resolve(res.data as ApiResponse<T>);
        } else {
          uni.showToast({ title: `请求错误: ${res.statusCode}`, icon: 'none' });
          reject(res);
        }
      },
      fail: (err) => {
        console.error('Request failed:', err);
        uni.showToast({ title: '网络连接失败', icon: 'none' });
        reject(err);
      },
    });
  });
};