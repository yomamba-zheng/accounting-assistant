<template>
  <view class="container">
    <!-- 统计卡片 -->
    <view class="stat-card">
      <view class="stat-header">
        <text class="stat-title">本月收支</text>
        <view class="header-btns">
          <view class="icon-btn" @click="goToAccount">💳</view>
        </view>
      </view>
      <view class="stat-content">
        <view class="stat-item" @click="filterType = 1">
          <text class="label" :class="{ active: filterType === 1 }">支出</text>
          <text class="num expense" :class="{ active: filterType === 1 }">¥{{ formatMoney(stats.totalExpense) }}</text>
        </view>
        <view class="stat-divider"></view>
        <view class="stat-item" @click="filterType = 2">
          <text class="label" :class="{ active: filterType === 2 }">收入</text>
          <text class="num income" :class="{ active: filterType === 2 }">¥{{ formatMoney(stats.totalIncome) }}</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-actions">
      <view class="action-item primary" @click="goToAdd">
        <text class="action-icon">✏️</text>
        <text class="action-text">记一笔</text>
      </view>
      <view class="action-item" @click="goToStats">
        <text class="action-icon">📊</text>
        <text class="action-text">看统计</text>
      </view>
      <view class="action-item" @click="showFilterModal = true">
        <text class="action-icon">🔍</text>
        <text class="action-text">筛选</text>
      </view>
      <view class="action-item" @click="goToBudget">
        <text class="action-icon">📝</text>
        <text class="action-text">预算</text>
      </view>
    </view>

    <!-- 列表标题 -->
    <view class="list-header">
      <text class="list-title">{{ filterType === 1 ? '支出' : filterType === 2 ? '收入' : '全部' }}明细</text>
      <text class="list-count">{{ filteredList.length }} 笔</text>
    </view>

    <!-- 账单列表 - 按日期分组 -->
    <view class="bill-list">
      <view v-if="loading && page === 1" class="loading">加载中...</view>
      <view v-else-if="groupedBills.length === 0" class="empty">
        <text class="empty-icon">📝</text>
        <text class="empty-text">暂无账单</text>
        <text class="empty-hint">点击上方"记一笔"开始记账吧</text>
      </view>

      <view v-for="group in groupedBills" :key="group.date" class="bill-group">
        <!-- 日期分组标题 -->
        <view class="group-header">
          <text class="group-date">{{ group.dateLabel }}</text>
          <text class="group-total">{{ group.total > 0 ? '-' : '+' }}¥{{ formatMoney(Math.abs(group.total)) }}</text>
        </view>

        <!-- 该日期的账单 -->
        <view v-for="item in group.bills" :key="item.id" class="bill-item" @click="showBillActions(item)">
          <view class="icon" :style="{ backgroundColor: item.category?.color || '#ccc' }">
            {{ item.category?.icon || '💰' }}
          </view>
          <view class="info">
            <text class="title">
              {{ item.category?.parent?.name ? item.category.parent.name + ' · ' : '' }}{{ item.category?.name || '未知分类' }}
            </text>
            <text class="remark">{{ item.remark || '' }}</text>
          </view>
          <view class="amount" :class="item.type === 1 ? 'expense' : 'income'">
            {{ item.type === 1 ? '-' : '+' }}¥{{ formatMoney(item.amount) }}
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="loading && page > 1" class="loading-more">加载中...</view>
      <view v-if="!hasMore && groupedBills.length > 0" class="no-more">没有更多了</view>
    </view>

    <!-- 底部记账按钮 -->
    <view class="fab" @click="goToAdd">
      <text class="fab-icon">+</text>
    </view>

    <!-- 筛选弹窗 -->
    <view v-if="showFilterModal" class="modal-mask" @click="showFilterModal = false">
      <view class="modal" @click.stop>
        <view class="modal-title">筛选条件</view>

        <view class="filter-section">
          <view class="filter-label">类型</view>
          <view class="filter-options">
            <view class="filter-btn" :class="{ active: filterType === 0 }" @click="filterType = 0">全部</view>
            <view class="filter-btn" :class="{ active: filterType === 1 }" @click="filterType = 1">支出</view>
            <view class="filter-btn" :class="{ active: filterType === 2 }" @click="filterType = 2">收入</view>
          </view>
        </view>

        <view class="filter-section">
          <view class="filter-label">时间范围</view>
          <view class="filter-options">
            <view class="filter-btn" :class="{ active: dateRange === 7 }" @click="dateRange = 7">最近7天</view>
            <view class="filter-btn" :class="{ active: dateRange === 30 }" @click="dateRange = 30">最近30天</view>
            <view class="filter-btn" :class="{ active: dateRange === 0 }" @click="dateRange = 0">本月</view>
          </view>
        </view>

        <view class="modal-btn-group">
          <view class="btn-reset" @click="resetFilter">重置</view>
          <view class="btn-confirm" @click="applyFilter">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { onShow, onPullDownRefresh, onReachBottom } from '@dcloudio/uni-app';
import { getBillList, deleteBill } from '@/api/bill';
import { Bill } from '@/types';
import { formatMoney, formatFullDate } from '@/utils/format';

interface BillGroup {
  date: string;
  dateLabel: string;
  bills: Bill[];
  total: number;
}

const billList = ref<Bill[]>([]);
const loading = ref(false);
const page = ref(1);
const size = 20;
const hasMore = ref(true);
const filterType = ref(0); // 0-全部 1-支出 2-收入
const dateRange = ref(7); // 7-7天 30-30天 0-本月
const showFilterModal = ref(false);
const stats = ref({ totalIncome: 0, totalExpense: 0 });

// 计算属性：根据筛选条件过滤账单
const filteredList = computed(() => {
  if (filterType.value === 0) return billList.value;
  return billList.value.filter(item => item.type === filterType.value);
});

// 按日期分组的账单
const groupedBills = computed((): BillGroup[] => {
  const groups: Map<string, Bill[]> = new Map();

  filteredList.value.forEach(item => {
    // 处理 ISO 格式或普通格式的日期
    let date = '';
    if (item.billTime) {
      date = item.billTime.split('T')[0].split(' ')[0];
    }
    if (!groups.has(date)) {
      groups.set(date, []);
    }
    groups.get(date)!.push(item);
  });

  const result: BillGroup[] = [];
  groups.forEach((bills, date) => {
    const total = bills.reduce((sum, item) => {
      return item.type === 1 ? sum - item.amount : sum + item.amount;
    }, 0);

    result.push({
      date,
      dateLabel: formatDateLabel(date),
      bills,
      total
    });
  });

  // 按日期倒序
  return result.sort((a, b) => b.date.localeCompare(a.date));
});

// 格式化日期标签 - 使用 YYYY-MM-DD 格式
const formatDateLabel = (dateStr: string) => {
  if (!dateStr) return '';
  const date = new Date(dateStr);
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${year}-${month}-${day}`;
};

// 获取筛选的开始时间
const getStartTime = () => {
  const now = new Date();
  if (dateRange.value === 0) {
    // 本月
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-01 00:00:00`;
  } else {
    // 最近N天
    const start = new Date(now);
    start.setDate(start.getDate() - dateRange.value);
    return start.toISOString().replace('T', ' ').split('.')[0];
  }
};

const loadData = async (reset = false) => {
  if (reset) {
    page.value = 1;
    billList.value = [];
    hasMore.value = true;
  }

  if (!hasMore.value && !reset) return;

  loading.value = true;
  try {
    const startTime = getStartTime();
    const res = await getBillList({
      page: page.value,
      size,
      startTime
    });

    if (res.code === 200 && res.data) {
      const newBills = res.data.records || [];

      if (reset) {
        billList.value = newBills;
      } else {
        billList.value = [...billList.value, ...newBills];
      }

      // 检查是否还有更多
      hasMore.value = newBills.length >= size;

      // 计算当月收支
      const allRes = await getBillList({ page: 1, size: 1000, startTime: getStartTime() });
      if (allRes.code === 200 && allRes.data) {
        let totalIncome = 0;
        let totalExpense = 0;
        allRes.data.records?.forEach((item: Bill) => {
          if (item.type === 2) totalIncome += item.amount;
          else if (item.type === 1) totalExpense += item.amount;
        });
        stats.value = { totalIncome, totalExpense };
      }
    }
  } catch (e) {
    console.error('加载数据失败:', e);
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
};

const loadMore = () => {
  if (hasMore.value && !loading.value) {
    page.value++;
    loadData(false);
  }
};

// 显示账单操作菜单
const showBillActions = (item: Bill) => {
  uni.showActionSheet({
    itemList: ['查看详情', '删除'],
    success: (res) => {
      if (res.tapIndex === 0) {
        // 查看详情
        uni.showModal({
          title: '账单详情',
          content: `分类：${item.category?.parent?.name ? item.category.parent.name + ' · ' : ''}${item.category?.name || '未知'}\n金额：¥${formatMoney(item.amount)}\n备注：${item.remark || '无'}\n时间：${formatFullDate(item.billTime)}`,
          showCancel: false,
          confirmText: '知道了'
        });
      } else if (res.tapIndex === 1) {
        // 删除
        handleDelete(item.id!);
      }
    }
  });
};

const handleDelete = (id: number) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这条账单吗？',
    success: async (res) => {
      if (res.confirm) {
        const r = await deleteBill(id);
        if (r.code === 200) {
          uni.showToast({ title: '删除成功', icon: 'success' });
          loadData(true);
        }
      }
    }
  });
};

const applyFilter = () => {
  showFilterModal.value = false;
  loadData(true);
};

const resetFilter = () => {
  filterType.value = 0;
  dateRange.value = 7;
};

const goToAccount = () => uni.switchTab({ url: '/pages/account/account' });
const goToAdd = () => uni.navigateTo({ url: '/pages/add/add' });
const goToStats = () => uni.switchTab({ url: '/pages/stats/stats' });
const goToBudget = () => uni.switchTab({ url: '/pages/budget/budget' });

onMounted(() => loadData(true));

onShow(() => loadData(true));

onPullDownRefresh(() => loadData(true));

onReachBottom(() => loadMore());
</script>

<style scoped>
.container {
  padding: 20rpx;
  padding-bottom: 180rpx;
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* 统计卡片 */
.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40rpx;
  border-radius: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(102, 126, 234, 0.3);
}
.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}
.stat-title {
  font-size: 28rpx;
  color: rgba(255,255,255,0.8);
}
.header-btns {
  display: flex;
  gap: 20rpx;
}
.icon-btn {
  font-size: 36rpx;
  padding: 10rpx;
  background: rgba(255,255,255,0.2);
  border-radius: 12rpx;
}
.stat-content {
  display: flex;
  justify-content: space-around;
  align-items: center;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  padding: 10rpx;
  border-radius: 12rpx;
}
.stat-item:active {
  background: rgba(255,255,255,0.1);
}
.stat-divider {
  width: 2rpx;
  height: 60rpx;
  background: rgba(255,255,255,0.3);
}
.label {
  font-size: 26rpx;
  color: rgba(255,255,255,0.7);
}
.label.active {
  color: #fff;
  font-weight: bold;
}
.num {
  font-size: 40rpx;
  font-weight: bold;
  margin-top: 10rpx;
  color: rgba(255,255,255,0.9);
}
.num.active {
  color: #fff;
}

/* 快捷入口 */
.quick-actions {
  display: flex;
  justify-content: space-between;
  background: #fff;
  padding: 24rpx;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}
.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}
.action-item.primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -24rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  color: #fff;
}
.action-icon {
  font-size: 44rpx;
  margin-bottom: 8rpx;
}
.action-text {
  font-size: 22rpx;
  color: #666;
}
.action-item.primary .action-text {
  color: #fff;
}

/* 列表标题 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 10rpx;
}
.list-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}
.list-count {
  font-size: 24rpx;
  color: #999;
}

/* 账单分组 */
.bill-group {
  margin-bottom: 20rpx;
}
.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 10rpx;
  background: #f5f5f5;
}
.group-date {
  font-size: 26rpx;
  color: #666;
  font-weight: bold;
}
.group-total {
  font-size: 26rpx;
  color: #999;
}

/* 账单列表 */
.bill-list {
  padding-bottom: 20rpx;
}
.bill-item {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 24rpx;
  border-radius: 12rpx;
  margin-bottom: 12rpx;
}
.icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  margin-right: 20rpx;
  flex-shrink: 0;
}
.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.title {
  font-size: 28rpx;
  font-weight: bold;
  color: #333;
}
.remark {
  font-size: 24rpx;
  color: #999;
  margin-top: 4rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.amount {
  font-size: 30rpx;
  font-weight: bold;
  margin-left: 16rpx;
}

/* 底部记账按钮 */
.fab {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  width: 120rpx;
  height: 120rpx;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 30rpx rgba(102, 126, 234, 0.5);
  z-index: 1000;
}
.fab-icon {
  font-size: 60rpx;
  color: #fff;
  font-weight: 300;
}

/* 空状态 */
.empty, .loading, .loading-more, .no-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
  color: #999;
}
.loading-more, .no-more {
  padding: 30rpx;
  font-size: 24rpx;
}
.empty-icon {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
.empty-text {
  font-size: 30rpx;
  color: #666;
  margin-bottom: 10rpx;
}
.empty-hint {
  font-size: 24rpx;
  color: #999;
}

/* 筛选弹窗 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}
.modal {
  background: #fff;
  width: 80%;
  border-radius: 20rpx;
  padding: 40rpx;
}
.modal-title {
  font-size: 34rpx;
  font-weight: bold;
  text-align: center;
  margin-bottom: 40rpx;
}
.filter-section {
  margin-bottom: 30rpx;
}
.filter-label {
  font-size: 28rpx;
  color: #333;
  margin-bottom: 16rpx;
}
.filter-options {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}
.filter-btn {
  padding: 16rpx 32rpx;
  background: #f5f5f5;
  border-radius: 30rpx;
  font-size: 26rpx;
  color: #666;
}
.filter-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.modal-btn-group {
  display: flex;
  justify-content: space-around;
  margin-top: 40rpx;
}
.btn-reset, .btn-confirm {
  padding: 20rpx 60rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}
.btn-reset {
  background: #f5f5f5;
  color: #666;
}
.btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
</style>
