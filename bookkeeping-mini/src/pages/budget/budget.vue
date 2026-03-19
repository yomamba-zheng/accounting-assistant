<template>
  <view class="container">
    <!-- 顶部导航 -->
    <view class="top-nav">
      <view class="nav-btn" @click="goBack">‹ 返回</view>
      <view class="nav-title">预算管理</view>
      <view class="nav-btn" @click="goHome">🏠</view>
    </view>

    <!-- 超支警告 -->
    <view v-if="overBudgetList.length > 0" class="warning-card">
      <text class="warning-icon">⚠️</text>
      <text class="warning-text">您有 {{ overBudgetList.length }} 项预算已超支！</text>
    </view>

    <!-- 预算列表 -->
    <view class="budget-list">
      <view v-if="loading" class="loading">加载中...</view>
      <view v-else-if="budgetList.length === 0" class="empty">
        暂无预算，快去设置吧！
      </view>

      <view v-for="item in budgetList" :key="item.id" class="budget-item">
        <view class="budget-header">
          <text class="budget-title">{{ item.category?.name || '总预算' }}</text>
          <text class="budget-period">{{ item.periodType === 'weekly' ? '本周' : '本月' }}</text>
        </view>

        <view class="budget-progress">
          <view class="progress-bar">
            <view
              class="progress-fill"
              :class="{ over: item.isOverBudget }"
              :style="{ width: Math.min(item.usedPercent || 0, 100) + '%' }"
            ></view>
          </view>
          <view class="progress-text">
            <text class="used">已用 ¥{{ (item.usedAmount || 0).toFixed(2) }}</text>
            <text class="total">/ 预算 ¥{{ item.amount.toFixed(2) }}</text>
          </view>
        </view>

        <view class="budget-footer">
          <text class="remaining" :class="{ over: item.isOverBudget }">
            {{ item.isOverBudget ? '已超支' : '剩余 ¥' + (item.remainingAmount || 0).toFixed(2) }}
          </text>
          <view class="delete-btn" @click="handleDelete(item.id)">删除</view>
        </view>
      </view>
    </view>

    <!-- 添加预算按钮 -->
    <view class="add-btn" @click="showAddModal = true">
      + 设置预算
    </view>

    <!-- 添加预算弹窗 -->
    <view v-if="showAddModal" class="modal-mask" @click="showAddModal = false">
      <view class="modal" @click.stop>
        <view class="modal-title">设置预算</view>

        <view class="form-group">
          <text class="label">预算类型</text>
          <picker :range="budgetTypes" range-key="label" @change="onTypeChange">
            <view class="picker">
              {{ currentType.label }}
            </view>
          </picker>
        </view>

        <view class="form-group">
          <text class="label">预算金额</text>
          <input v-model="form.amount" type="digit" class="input" placeholder="请输入预算金额" />
        </view>

        <view class="form-group" v-if="currentType.value !== 'total'">
          <text class="label">选择分类</text>
          <picker :range="categories" range-key="name" @change="onCategoryChange">
            <view class="picker">
              {{ currentCategory?.name || '选择分类' }}
            </view>
          </picker>
        </view>

        <view class="modal-btn-group">
          <view class="btn-cancel" @click="showAddModal = false">取消</view>
          <view class="btn-confirm" @click="handleSubmit">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { getBudgetList, setBudget, deleteBudget, checkOverBudget } from '@/api/budget';
import { getCategoryList } from '@/api/category';
import { Budget, Category } from '@/types';

const loading = ref(false);
const budgetList = ref<Budget[]>([]);
const overBudgetList = ref<Budget[]>([]);
const categories = ref<Category[]>([]);
const showAddModal = ref(false);
const form = ref({ amount: '', categoryId: undefined as any });

const goBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
  } else {
    uni.switchTab({ url: '/pages/index/index' });
  }
};
const goHome = () => uni.switchTab({ url: '/pages/index/index' });

const budgetTypes = [
  { label: '总预算', value: 'total' },
  { label: '分类预算', value: 'category' }
];

const currentType = ref(budgetTypes[0]);

const currentCategory = computed(() =>
  categories.value.find(c => c.id === form.value.categoryId)
);

const loadData = async () => {
  loading.value = true;
  try {
    const [budgetRes, categoryRes, overRes] = await Promise.all([
      getBudgetList(),
      getCategoryList(1),
      checkOverBudget()
    ]);

    if (budgetRes.code === 200) {
      budgetList.value = budgetRes.data || [];
    }
    if (categoryRes.code === 200) {
      categories.value = categoryRes.data || [];
    }
    if (overRes.code === 200) {
      overBudgetList.value = overRes.data || [];
    }
  } catch (error) {
    console.error('加载预算失败:', error);
  } finally {
    loading.value = false;
  }
};

const onTypeChange = (e: any) => {
  currentType.value = budgetTypes[e.detail.value];
  form.value.categoryId = undefined;
};

const onCategoryChange = (e: any) => {
  form.value.categoryId = categories.value[e.detail.value].id;
};

const handleSubmit = async () => {
  if (!form.value.amount) {
    uni.showToast({ title: '请输入预算金额', icon: 'none' });
    return;
  }

  try {
    const data = {
      amount: parseFloat(form.value.amount),
      categoryId: currentType.value.value === 'category' ? form.value.categoryId : undefined,
      periodType: 'monthly'
    };

    const res = await setBudget(data);
    if (res.code === 200) {
      uni.showToast({ title: '设置成功', icon: 'success' });
      showAddModal.value = false;
      form.value = { amount: '', categoryId: undefined as any };
      loadData();
    }
  } catch (error) {
    console.error('设置预算失败:', error);
    uni.showToast({ title: '设置失败', icon: 'none' });
  }
};

const handleDelete = (id: number) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这笔预算吗？',
    success: async (res) => {
      if (res.confirm) {
        const r = await deleteBudget(id);
        if (r.code === 200) {
          uni.showToast({ title: '删除成功', icon: 'success' });
          loadData();
        }
      }
    }
  });
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.container {
  padding: 20rpx;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 120rpx;
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

.warning-card {
  display: flex;
  align-items: center;
  background: #fff2e8;
  padding: 20rpx 30rpx;
  border-radius: 12rpx;
  margin-bottom: 20rpx;
  border-left: 8rpx solid #ff4d4f;
}

.warning-icon {
  font-size: 32rpx;
  margin-right: 15rpx;
}

.warning-text {
  color: #ff4d4f;
  font-size: 26rpx;
}

.budget-list {
  margin-bottom: 20rpx;
}

.empty, .loading {
  text-align: center;
  color: #999;
  padding: 100rpx 0;
  font-size: 28rpx;
}

.budget-item {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.budget-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.budget-title {
  font-size: 30rpx;
  font-weight: bold;
  color: #333;
}

.budget-period {
  font-size: 24rpx;
  color: #999;
}

.budget-progress {
  margin-bottom: 20rpx;
}

.progress-bar {
  height: 16rpx;
  background: #f0f0f0;
  border-radius: 8rpx;
  overflow: hidden;
  margin-bottom: 15rpx;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea, #764ba2);
  border-radius: 8rpx;
  transition: width 0.3s;
}

.progress-fill.over {
  background: linear-gradient(90deg, #ff4d4f, #ff7875);
}

.progress-text {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
}

.used {
  color: #333;
}

.total {
  color: #999;
}

.budget-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.remaining {
  font-size: 26rpx;
  color: #52c41a;
}

.remaining.over {
  color: #ff4d4f;
}

.delete-btn {
  font-size: 24rpx;
  color: #999;
  padding: 10rpx 20rpx;
}

.add-btn {
  position: fixed;
  bottom: 40rpx;
  left: 50%;
  transform: translateX(-50%);
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 25rpx 80rpx;
  border-radius: 50rpx;
  font-size: 30rpx;
  box-shadow: 0 8rpx 20rpx rgba(102, 126, 234, 0.4);
}

/* 弹窗样式 */
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
  z-index: 1000;
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

.form-group {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
}

.input, .picker {
  flex: 1;
  font-size: 28rpx;
  text-align: right;
}

.modal-btn-group {
  display: flex;
  justify-content: space-around;
  margin-top: 40rpx;
}

.btn-cancel, .btn-confirm {
  padding: 20rpx 60rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
}

.btn-cancel {
  background: #f5f5f5;
  color: #666;
}

.btn-confirm {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
</style>
