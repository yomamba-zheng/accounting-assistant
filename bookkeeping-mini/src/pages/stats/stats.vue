<template>
  <view class="container">
    <!-- 顶部导航 -->
    <view class="top-nav">
      <view class="nav-btn" @click="goBack">‹ 返回</view>
      <view class="nav-title">数据统计</view>
      <view class="nav-btn" @click="goHome">🏠</view>
    </view>

    <!-- 时间筛选 -->
    <view class="time-filter">
      <view
        v-for="item in timeOptions"
        :key="item.value"
        class="time-btn"
        :class="{ active: currentRange === item.value }"
        @click="changeRange(item.value)"
      >
        {{ item.label }}
      </view>
    </view>

    <!-- 核心统计卡片 -->
    <view class="stat-overview">
      <view class="overview-item income">
        <text class="overview-label">总收入</text>
        <text class="overview-value">¥{{ formatMoney(statsData.totalIncome) }}</text>
        <text class="overview-trend" v-if="statsData.incomeChange !== 0">
          {{ statsData.incomeChange > 0 ? '↑' : '↓' }} {{ Math.abs(statsData.incomeChange).toFixed(1) }}%
        </text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-item expense">
        <text class="overview-label">总支出</text>
        <text class="overview-value">¥{{ formatMoney(statsData.totalExpense) }}</text>
        <text class="overview-trend" v-if="statsData.expenseChange !== 0">
          {{ statsData.expenseChange > 0 ? '↑' : '↓' }} {{ Math.abs(statsData.expenseChange).toFixed(1) }}%
        </text>
      </view>
      <view class="overview-divider"></view>
      <view class="overview-item balance">
        <text class="overview-label">结余</text>
        <text class="overview-value" :class="statsData.balance >= 0 ? 'income' : 'expense'">
          ¥{{ formatMoney(Math.abs(statsData.balance)) }}
        </text>
      </view>
    </view>

    <!-- 日均消费 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">消费分析</text>
      </view>
      <view class="analysis-grid">
        <view class="analysis-item">
          <text class="analysis-label">日均消费</text>
          <text class="analysis-value">¥{{ formatMoney(statsData.dailyAvg) }}</text>
        </view>
        <view class="analysis-item">
          <text class="analysis-label">单笔最大</text>
          <text class="analysis-value">¥{{ formatMoney(statsData.maxExpense) }}</text>
        </view>
        <view class="analysis-item">
          <text class="analysis-label">消费笔数</text>
          <text class="analysis-value">{{ statsData.expenseCount }} 笔</text>
        </view>
        <view class="analysis-item">
          <text class="analysis-label">收入笔数</text>
          <text class="analysis-value">{{ statsData.incomeCount }} 笔</text>
        </view>
      </view>
    </view>

    <!-- 分类支出占比 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">支出分类</text>
        <view class="category-toggle">
          <text
            class="toggle-btn"
            :class="{ active: categoryType === 'small' }"
            @click="categoryType = 'small'"
          >小类</text>
          <text
            class="toggle-btn"
            :class="{ active: categoryType === 'large' }"
            @click="categoryType = 'large'"
          >大类</text>
        </view>
      </view>
      <view class="category-list" v-if="categoryData.length > 0">
        <view v-for="(item, index) in categoryData" :key="index" class="category-item">
          <view class="category-left">
            <view class="category-rank">{{ index + 1 }}</view>
            <view class="category-info">
              <text class="category-name">{{ item.categoryName }}</text>
              <view class="category-bar">
                <view class="category-bar-fill" :style="{ width: item.percentage + '%', backgroundColor: item.categoryColor }"></view>
              </view>
            </view>
          </view>
          <view class="category-right">
            <text class="category-amount">¥{{ formatMoney(item.totalAmount) }}</text>
            <text class="category-percent">{{ item.percentage.toFixed(1) }}%</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-tip">暂无支出数据</view>
    </view>

    <!-- 收入分类 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">收入分类</text>
      </view>
      <view class="category-list" v-if="incomeCategoryData.length > 0">
        <view v-for="(item, index) in incomeCategoryData" :key="index" class="category-item">
          <view class="category-left">
            <view class="category-info">
              <text class="category-name">{{ item.categoryName }}</text>
            </view>
          </view>
          <view class="category-right">
            <text class="category-amount income">+¥{{ formatMoney(item.totalAmount) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-tip">暂无收入数据</view>
    </view>

    <!-- 每日趋势 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">每日支出趋势</text>
      </view>
      <view class="trend-chart" v-if="dailyTrend.length > 0">
        <view class="trend-bars">
          <view v-for="(day, index) in dailyTrend" :key="index" class="trend-bar-item">
            <view class="trend-bar-wrapper">
              <view
                class="trend-bar"
                :style="{ height: getTrendBarHeight(day.amount) + '%' }"
              ></view>
            </view>
            <text class="trend-label">{{ day.day }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-tip">暂无数据</view>
    </view>

    <!-- 月度对比 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">月度对比</text>
      </view>
      <view class="monthly-comparison" v-if="monthlyComparison.length > 0">
        <view v-for="(month, index) in monthlyComparison" :key="index" class="month-item">
          <text class="month-label">{{ month.month }}</text>
          <view class="month-bars">
            <view class="month-bar-group">
              <view class="month-bar expense" :style="{ width: getMonthBarWidth(month.expense) + '%' }"></view>
            </view>
            <view class="month-bar-group">
              <view class="month-bar income" :style="{ width: getMonthBarWidth(month.income) + '%' }"></view>
            </view>
          </view>
          <view class="month-values">
            <text class="expense">¥{{ formatMoney(month.expense) }}</text>
            <text class="income">¥{{ formatMoney(month.income) }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-tip">暂无数据</view>
    </view>

    <!-- 加载状态 -->
    <view v-if="loading" class="loading">加载中...</view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { onPullDownRefresh } from '@dcloudio/uni-app';
import { getBillList, getVisualizationData, type VisualizationParams, type BillListParams } from '@/api/bill';
import { formatMoney } from '@/utils/format';

interface StatsData {
  totalIncome: number;
  totalExpense: number;
  balance: number;
  dailyAvg: number;
  maxExpense: number;
  expenseCount: number;
  incomeCount: number;
  incomeChange: number;
  expenseChange: number;
}

interface CategoryItem {
  categoryName: string;
  categoryColor: string;
  totalAmount: number;
  percentage: number;
}

interface DailyTrendItem {
  day: string;
  amount: number;
}

interface MonthlyItem {
  month: string;
  expense: number;
  income: number;
}

const loading = ref(false);
const currentRange = ref('month');
const statsData = ref<StatsData>({
  totalIncome: 0,
  totalExpense: 0,
  balance: 0,
  dailyAvg: 0,
  maxExpense: 0,
  expenseCount: 0,
  incomeCount: 0,
  incomeChange: 0,
  expenseChange: 0
});
const categoryData = ref<CategoryItem[]>([]);
const categoryType = ref<'small' | 'large'>('small'); // 小类/大类统计
const incomeCategoryData = ref<CategoryItem[]>([]);
const dailyTrend = ref<DailyTrendItem[]>([]);
const monthlyComparison = ref<MonthlyItem[]>([]);
const maxDailyAmount = ref(0);
const maxMonthAmount = ref(0);

const goBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
  } else {
    uni.switchTab({ url: '/pages/index/index' });
  }
};
const goHome = () => uni.switchTab({ url: '/pages/index/index' });

const timeOptions = [
  { label: '本周', value: 'week' },
  { label: '本月', value: 'month' },
  { label: '本季度', value: 'quarter' },
  { label: '本年', value: 'year' }
];

// 获取时间范围
const getDateRange = () => {
  const now = new Date();
  let startTime: string;
  let prevStartTime: string;

  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');

  if (currentRange.value === 'week') {
    const weekStart = new Date(now);
    weekStart.setDate(weekStart.getDate() - 6);
    startTime = `${weekStart.getFullYear()}-${String(weekStart.getMonth() + 1).padStart(2, '0')}-${String(weekStart.getDate()).padStart(2, '0')} 00:00:00`;
    const prevWeekStart = new Date(weekStart);
    prevWeekStart.setDate(prevWeekStart.getDate() - 7);
    prevStartTime = `${prevWeekStart.getFullYear()}-${String(prevWeekStart.getMonth() + 1).padStart(2, '0')}-${String(prevWeekStart.getDate()).padStart(2, '0')} 00:00:00`;
  } else if (currentRange.value === 'month') {
    startTime = `${year}-${month}-01 00:00:00`;
    const prevMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    prevStartTime = `${prevMonth.getFullYear()}-${String(prevMonth.getMonth() + 1).padStart(2, '0')}-01 00:00:00`;
  } else if (currentRange.value === 'quarter') {
    const quarterStartMonth = Math.floor(now.getMonth() / 3) * 3;
    const quarterStart = new Date(now.getFullYear(), quarterStartMonth, 1);
    startTime = `${quarterStart.getFullYear()}-${String(quarterStart.getMonth() + 1).padStart(2, '0')}-01 00:00:00`;
    const prevQuarter = new Date(now.getFullYear(), quarterStartMonth - 3, 1);
    prevStartTime = `${prevQuarter.getFullYear()}-${String(prevQuarter.getMonth() + 1).padStart(2, '0')}-01 00:00:00`;
  } else {
    startTime = `${year}-01-01 00:00:00`;
    prevStartTime = `${year - 1}-01-01 00:00:00`;
  }

  return { startTime, prevStartTime };
};

// 加载统计数据
const loadData = async () => {
  loading.value = true;
  try {
    const { startTime, prevStartTime } = getDateRange();
    const now = new Date();

    // 获取当前周期数据
    const currentRes = await getBillList({ page: 1, size: 1000, startTime });
    const currentBills = currentRes.data?.records || [];

    // 获取上周期数据（用于对比）
    const prevRes = await getBillList({ page: 1, size: 1000, startTime: prevStartTime });
    const prevBills = prevRes.data?.records || [];

    // 计算当前周期统计
    let totalIncome = 0;
    let totalExpense = 0;
    let maxExpense = 0;
    let expenseCount = 0;
    let incomeCount = 0;

    const incomeCategoryMap = new Map<string, number>();

    currentBills.forEach((bill: any) => {
      if (bill.type === 2) {
        totalIncome += bill.amount;
        incomeCount++;
        const name = bill.category?.name || '其他';
        incomeCategoryMap.set(name, (incomeCategoryMap.get(name) || 0) + bill.amount);
      } else {
        totalExpense += bill.amount;
        expenseCount++;
        if (bill.amount > maxExpense) maxExpense = bill.amount;
      }
    });

    // 计算上周期统计
    let prevIncome = 0;
    let prevExpense = 0;
    prevBills.forEach((bill: any) => {
      if (bill.type === 2) {
        prevIncome += bill.amount;
      } else {
        prevExpense += bill.amount;
      }
    });

    // 计算变化百分比
    const incomeChange = prevIncome > 0 ? ((totalIncome - prevIncome) / prevIncome) * 100 : 0;
    const expenseChange = prevExpense > 0 ? ((totalExpense - prevExpense) / prevExpense) * 100 : 0;

    // 计算天数
    let days = 1;
    if (currentRange.value === 'week') days = 7;
    else if (currentRange.value === 'month') days = now.getDate();
    else if (currentRange.value === 'quarter') days = Math.min(90, now.getDate() + (now.getMonth() % 3) * 30);
    else days = 365;

    statsData.value = {
      totalIncome,
      totalExpense,
      balance: totalIncome - totalExpense,
      dailyAvg: totalExpense / days,
      maxExpense,
      expenseCount,
      incomeCount,
      incomeChange,
      expenseChange
    };

    // 分类支出数据 - 支持大类/小类统计
    let categoryGroups: Map<string, { amount: number; color: string }> = new Map();

    if (categoryType.value === 'large') {
      // 按大类分组
      currentBills.forEach((bill: any) => {
        if (bill.type === 1) { // 只统计支出
          const largeName = bill.category?.parent?.name || bill.category?.name || '其他';
          const color = bill.category?.parent?.color || bill.category?.color || '#667eea';
          const existing = categoryGroups.get(largeName) || { amount: 0, color };
          existing.amount += bill.amount;
          categoryGroups.set(largeName, existing);
        }
      });
    } else {
      // 按小类分组
      currentBills.forEach((bill: any) => {
        if (bill.type === 1) {
          const name = bill.category?.name || '其他';
          const color = bill.category?.color || '#667eea';
          const existing = categoryGroups.get(name) || { amount: 0, color };
          existing.amount += bill.amount;
          categoryGroups.set(name, existing);
        }
      });
    }

    const sortedCategories = Array.from(categoryGroups.entries())
      .map(([name, data]) => ({
        categoryName: name,
        categoryColor: data.color,
        totalAmount: data.amount,
        percentage: 0
      }))
      .sort((a, b) => b.totalAmount - a.totalAmount);

    sortedCategories.forEach(item => {
      item.percentage = totalExpense > 0 ? (item.totalAmount / totalExpense) * 100 : 0;
    });
    categoryData.value = sortedCategories.slice(0, 5);

    // 分类收入数据
    incomeCategoryData.value = Array.from(incomeCategoryMap.entries())
      .map(([name, amount]) => ({
        categoryName: name,
        categoryColor: '#52c41a',
        totalAmount: amount,
        percentage: totalIncome > 0 ? (amount / totalIncome) * 100 : 0
      }))
      .sort((a, b) => b.totalAmount - a.totalAmount);

    // 每日趋势
    const dailyMap = new Map<string, number>();
    currentBills.forEach((bill: any) => {
      if (bill.type === 1) {
        const day = bill.billTime?.split(' ')[0]?.slice(-2) || '01';
        dailyMap.set(day, (dailyMap.get(day) || 0) + bill.amount);
      }
    });
    dailyTrend.value = Array.from(dailyMap.entries())
      .map(([day, amount]) => ({ day, amount }))
      .sort((a, b) => a.day.localeCompare(b.day));
    maxDailyAmount.value = Math.max(...dailyTrend.value.map(d => d.amount), 1);

    // 月度对比
    const monthMap = new Map<string, { expense: number; income: number }>();
    const months = currentRange.value === 'year' ? 12 : currentRange.value === 'quarter' ? 3 : currentRange.value === 'month' ? 1 : 4;
    for (let i = 0; i < months; i++) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      monthMap.set(key, { expense: 0, income: 0 });
    }
    currentBills.forEach((bill: any) => {
      const key = bill.billTime?.split(' ')[0]?.slice(0, 7) || '';
      if (monthMap.has(key)) {
        const data = monthMap.get(key)!;
        if (bill.type === 2) data.income += bill.amount;
        else data.expense += bill.amount;
      }
    });
    monthlyComparison.value = Array.from(monthMap.entries())
      .map(([month, data]) => ({ month: month.slice(5) + '月', ...data }))
      .reverse();
    maxMonthAmount.value = Math.max(...monthlyComparison.value.map(m => Math.max(m.expense, m.income)), 1);

  } catch (error) {
    console.error('加载统计数据失败:', error);
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const getTrendBarHeight = (amount: number): number => {
  return maxDailyAmount.value > 0 ? (amount / maxDailyAmount.value) * 100 : 0;
};

const getMonthBarWidth = (amount: number): number => {
  return maxMonthAmount.value > 0 ? (amount / maxMonthAmount.value) * 100 : 0;
};

const changeRange = (value: string) => {
  currentRange.value = value;
  loadData();
};

onMounted(() => {
  loadData();
});

// 监听分类类型变化
watch(categoryType, () => {
  loadData();
});

onPullDownRefresh(() => {
  loadData();
});
</script>

<style scoped>
.container {
  padding: 20rpx;
  background-color: #f5f5f5;
  min-height: 100vh;
  padding-bottom: 40rpx;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  padding-top: calc(20rpx + var(--status-bar-height));
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -20rpx -20rpx 20rpx -20rpx;
}
.nav-btn {
  color: #fff;
  font-size: 32rpx;
  padding: 16rpx 24rpx;
  font-weight: bold;
}
.nav-title {
  color: #fff;
  font-size: 34rpx;
  font-weight: bold;
}

/* 时间筛选 */
.time-filter {
  display: flex;
  gap: 16rpx;
  margin-bottom: 20rpx;
  flex-wrap: wrap;
}
.time-btn {
  padding: 16rpx 32rpx;
  background: #fff;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
}
.time-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* 核心统计 */
.stat-overview {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 20rpx;
  margin-bottom: 20rpx;
  align-items: center;
}
.overview-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.overview-divider {
  width: 2rpx;
  height: 80rpx;
  background: #eee;
}
.overview-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}
.overview-value {
  font-size: 36rpx;
  font-weight: bold;
}
.overview-value.income { color: #52c41a; }
.overview-value.expense { color: #ff6b6b; }
.overview-trend {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}

/* 统计卡片 */
.stat-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}
.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}
.stat-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}
.stat-subtitle {
  font-size: 24rpx;
  color: #999;
}

.category-toggle {
  display: flex;
  gap: 8rpx;
}
.toggle-btn {
  padding: 8rpx 20rpx;
  font-size: 24rpx;
  color: #999;
  background: #f5f5f5;
  border-radius: 20rpx;
}
.toggle-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

/* 消费分析 */
.analysis-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
}
.analysis-item {
  display: flex;
  flex-direction: column;
  padding: 20rpx;
  background: #f8f8f8;
  border-radius: 12rpx;
}
.analysis-label {
  font-size: 24rpx;
  color: #999;
  margin-bottom: 8rpx;
}
.analysis-value {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

/* 分类列表 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.category-left {
  display: flex;
  align-items: center;
  flex: 1;
}
.category-rank {
  width: 40rpx;
  height: 40rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  color: #fff;
  font-size: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16rpx;
}
.category-info {
  flex: 1;
}
.category-name {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 8rpx;
  display: block;
}
.category-bar {
  height: 12rpx;
  background: #f0f0f0;
  border-radius: 6rpx;
  overflow: hidden;
}
.category-bar-fill {
  height: 100%;
  border-radius: 6rpx;
  transition: width 0.3s;
}
.category-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  margin-left: 20rpx;
}
.category-amount {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}
.category-amount.income {
  color: #52c41a;
}
.category-percent {
  font-size: 24rpx;
  color: #999;
}

/* 每日趋势 */
.trend-chart {
  padding: 20rpx 0;
}
.trend-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 200rpx;
}
.trend-bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.trend-bar-wrapper {
  height: 150rpx;
  width: 30rpx;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}
.trend-bar {
  width: 100%;
  background: linear-gradient(to top, #667eea, #764ba2);
  border-radius: 6rpx 6rpx 0 0;
  min-height: 4rpx;
}
.trend-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 10rpx;
}

/* 月度对比 */
.monthly-comparison {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.month-item {
  display: flex;
  align-items: center;
}
.month-label {
  width: 80rpx;
  font-size: 26rpx;
  color: #666;
}
.month-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}
.month-bar-group {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
}
.month-bar {
  height: 100%;
  border-radius: 8rpx;
}
.month-bar.expense { background: #ff6b6b; }
.month-bar.income { background: #52c41a; }
.month-values {
  width: 180rpx;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 22rpx;
}
.month-values .expense { color: #ff6b6b; }
.month-values .income { color: #52c41a; }

.empty-tip {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
  font-size: 26rpx;
}

.loading {
  text-align: center;
  color: #999;
  padding: 40rpx;
}
</style>
