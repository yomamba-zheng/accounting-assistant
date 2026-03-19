// 统一响应结构
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 分类实体
export interface Category {
  id: number;
  name: string;
  icon: string;
  color: string;
  type: 1 | 2; // 1: 支出，2: 收入
  parentId?: number;
  parent?: Category; // 父分类（大类）
}

// 账单实体
export interface Bill {
  id: number;
  amount: number;
  categoryId: number;
  remark: string;
  billTime: string; // ISO 字符串
  type?: 1 | 2; // 1-支出，2-收入
  category?: Category;
  createTime?: string;
}

// 快速记账预览结果
export interface QuickAddPreview {
  amount: number;
  categoryId?: number;
  remark: string;
  billTime?: string;
  category?: Category;
}

// 统计信息
export interface StatTotal {
  totalIncome: number;
  totalExpense: number;
  balance: number;
}

// 分类统计
export interface CategoryStat {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
}

// 分类占比（饼图数据）
export interface CategoryPercentage {
  categoryId: number;
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
  percentage: number;
}

// 月度趋势
export interface MonthlyTrend {
  yearMonth: string;
  monthLabel: string;
  totalAmount: number;
}

// 可视化数据
export interface VisualizationData {
  totalExpense: number;
  categoryStats: CategoryStat[];
  categoryPercentages: CategoryPercentage[];
  monthlyTrends: MonthlyTrend[];
}

// 预算
export interface Budget {
  id: number;
  userId: number;
  amount: number;
  categoryId?: number;
  periodType: string;
  startTime: string;
  endTime: string;
  usedAmount?: number;
  remainingAmount?: number;
  usedPercent?: number;
  isOverBudget?: boolean;
  category?: Category;
}

// 账户
export interface Account {
  id: number;
  userId: number;
  name: string;
  type: string;
  balance: number;
  initialBalance: number;
  icon: string;
  color: string;
  sortOrder: number;
  isDefault: boolean;
  remark?: string;
}

// 账户变动记录
export interface AccountRecord {
  id: number;
  accountId: number;
  userId: number;
  billId?: number;
  amount: number;
  beforeBalance: number;
  afterBalance: number;
  type: string;
  remark: string;
  createdAt: string;
}