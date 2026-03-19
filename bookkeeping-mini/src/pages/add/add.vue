<template>
  <view class="container">
    <!-- 顶部导航 -->
    <view class="top-nav">
      <view class="nav-btn" @click="goBack">‹ 返回</view>
      <view class="nav-title">记一笔</view>
      <view class="nav-btn" @click="goHome">🏠</view>
    </view>
    <!-- 模式切换 -->
    <view class="mode-switch">
      <view
        class="switch-btn"
        :class="{ active: mode === 'quick' }"
        @click="mode = 'quick'"
      >
        ⚡ 快速记账
      </view>
      <view
        class="switch-btn"
        :class="{ active: mode === 'manual' }"
        @click="mode = 'manual'"
      >
        📝 手动记账
      </view>
    </view>

    <!-- 类型切换：支出/收入 -->
    <view class="type-switch">
      <view
        class="type-btn expense"
        :class="{ active: billType === 1 }"
        @click="billType = 1"
      >
        支出
      </view>
      <view
        class="type-btn income"
        :class="{ active: billType === 2 }"
        @click="billType = 2"
      >
        收入
      </view>
    </view>

    <!-- 快速记账模式 -->
    <view v-if="mode === 'quick'" class="mode-content">
      <view class="quick-input-box">
        <textarea
          v-model="inputText"
          placeholder="输入消费内容，如：午餐 25 元"
          class="textarea"
          @confirm="handleQuickSubmit"
        />
      </view>

      <!-- 识别结果预览 -->
      <view v-if="previewData" class="preview-card">
        <view class="preview-title">识别结果</view>
        <view class="preview-content">
          <view class="preview-item">
            <text class="label">金额</text>
            <text class="value expense">¥{{ previewData.amount }}</text>
          </view>
          <view class="preview-item">
            <text class="label">分类</text>
            <text class="value">{{ previewData.category?.parent?.name ? previewData.category.parent.name + ' · ' : '' }}{{ previewData.category?.name || '未识别' }}</text>
          </view>
          <view class="preview-item" v-if="previewData.remark">
            <text class="label">备注</text>
            <text class="value">{{ previewData.remark }}</text>
          </view>
        </view>
      </view>

      <!-- 快捷金额按钮 -->
      <view class="quick-amounts" v-if="!previewData?.amount">
        <view
          v-for="item in quickAmounts"
          :key="item"
          class="quick-amount-btn"
          @click="addQuickAmount(item)"
        >
          {{ item }}元
        </view>
      </view>

      <!-- 日期选择 -->
      <picker mode="date" :value="billDate" @change="onDateChange">
        <view class="form-group">
          <text class="label">📅 日期</text>
          <text class="picker">{{ formatDate(billDate) }}</text>
        </view>
      </picker>

      <view class="btn-primary" @click="handleQuickSubmit">
        快速记账
      </view>
    </view>

    <!-- 手动记账模式 -->
    <view v-if="mode === 'manual'" class="mode-content">
      <view class="form-group">
        <text class="label">💰 金额</text>
        <input v-model="form.amount" type="digit" class="input" placeholder="0.00" />
      </view>

      <view class="form-group">
        <text class="label">📂 分类</text>
        <picker :range="categories" range-key="name" @change="onCategoryChange">
          <view class="picker">
            {{ currentCategory?.name || '选择分类' }}
          </view>
        </picker>
      </view>

      <picker mode="date" :value="billDate" @change="onDateChange">
        <view class="form-group">
          <text class="label">📅 日期</text>
          <text class="picker">{{ formatDate(billDate) }}</text>
        </view>
      </picker>

      <view class="form-group">
        <text class="label">📝 备注</text>
        <input v-model="form.remark" class="input" placeholder="可选" />
      </view>

      <view class="btn-primary" @click="handleManualSubmit">
        确认记账
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { quickAdd, addBill, previewQuickAdd } from '@/api/bill';
import { getCategoryList } from '@/api/category';
import { Category, QuickAddPreview } from '@/types';
import { formatFullDate } from '@/utils/format';

const mode = ref<'quick' | 'manual'>('quick');
const billType = ref<1 | 2>(1); // 1=支出 2=收入
const inputText = ref('');
const previewData = ref<QuickAddPreview | null>(null);
const categories = ref<Category[]>([]);
const form = ref({ amount: '', remark: '', categoryId: undefined as any });
const billDate = ref(new Date().toISOString().split('T')[0]);
const submitting = ref(false);

const quickAmounts = [10, 20, 50, 100, 200, 500];

const goBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
  } else {
    uni.switchTab({ url: '/pages/index/index' });
  }
};
const goHome = () => uni.switchTab({ url: '/pages/index/index' });

const currentCategory = computed(() =>
  categories.value.find(c => c.id === form.value.categoryId)
);

// 加载分类
const loadCategories = async () => {
  const res = await getCategoryList(billType.value);
  if (res.code === 200) categories.value = res.data || [];
  // 切换类型时清空已选分类
  form.value.categoryId = undefined;
};

onMounted(() => {
  loadCategories();
});

// 监听类型变化，重新加载分类
watch(billType, () => {
  loadCategories();
});

// 监听快速记账输入
watch(inputText, async (newVal) => {
  if (mode.value !== 'quick') return;
  if (!newVal?.trim()) {
    previewData.value = null;
    return;
  }
  try {
    const res = await previewQuickAdd(newVal.trim());
    if (res.code === 200 && res.data) {
      previewData.value = res.data;
    }
  } catch (error) {
    console.error('识别失败:', error);
  }
});

const formatDate = (dateStr: string) => {
  return formatFullDate(dateStr + ' 00:00:00');
};

const onDateChange = (e: any) => {
  billDate.value = e.detail.value;
};

const addQuickAmount = (amount: number) => {
  inputText.value = inputText.value + ' ' + amount + '元';
};

const onCategoryChange = (e: any) => {
  form.value.categoryId = categories.value[e.detail.value].id;
};

// 快速记账提交
const handleQuickSubmit = async () => {
  if (!inputText.value?.trim()) {
    uni.showToast({ title: '请输入消费内容', icon: 'none' });
    return;
  }

  submitting.value = true;
  try {
    const res = await quickAdd(inputText.value.trim(), undefined, billDate.value + ' 00:00:00', billType.value);
    if (res.code === 200) {
      uni.showToast({ title: billType.value === 1 ? '支出记录成功' : '收入记录成功', icon: 'success' });
      inputText.value = '';
      previewData.value = null;
      setTimeout(() => uni.navigateBack(), 1000);
    }
  } catch (error: any) {
    uni.showToast({ title: error?.response?.data?.message || '记账失败', icon: 'none' });
  } finally {
    submitting.value = false;
  }
};

// 手动记账提交
const handleManualSubmit = async () => {
  if (!form.value.amount) {
    uni.showToast({ title: '请输入金额', icon: 'none' });
    return;
  }
  if (!form.value.categoryId) {
    uni.showToast({ title: '请选择分类', icon: 'none' });
    return;
  }

  submitting.value = true;
  try {
    const res = await addBill({
      amount: parseFloat(form.value.amount),
      categoryId: form.value.categoryId,
      remark: form.value.remark,
      billTime: billDate.value + ' 00:00:00',
      type: billType.value
    });
    if (res.code === 200) {
      uni.showToast({ title: billType.value === 1 ? '支出记录成功' : '收入记录成功', icon: 'success' });
      form.value = { amount: '', remark: '', categoryId: undefined as any };
      setTimeout(() => uni.navigateBack(), 1000);
    }
  } catch (error: any) {
    uni.showToast({ title: error?.response?.data?.message || '记账失败', icon: 'none' });
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx;
  padding-top: calc(20rpx + var(--status-bar-height));
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  margin: -30rpx -30rpx 20rpx -30rpx;
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

.container {
  padding: 30rpx;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.mode-switch {
  display: flex;
  background: #fff;
  border-radius: 50rpx;
  padding: 8rpx;
  margin-bottom: 20rpx;
}

.type-switch {
  display: flex;
  background: #fff;
  border-radius: 50rpx;
  padding: 8rpx;
  margin-bottom: 30rpx;
}

.type-btn {
  flex: 1;
  text-align: center;
  padding: 16rpx;
  border-radius: 40rpx;
  font-size: 26rpx;
  color: #999;
  transition: all 0.3s;
}

.type-btn.expense.active {
  background: #ff6b6b;
  color: #fff;
}

.type-btn.income.active {
  background: #52c41a;
  color: #fff;
}

.switch-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #666;
}

.switch-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.mode-content {
  min-height: 60vh;
}

.quick-input-box {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.textarea {
  width: 100%;
  height: 120rpx;
  font-size: 36rpx;
}

.preview-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
}

.preview-title {
  color: rgba(255,255,255,0.8);
  font-size: 26rpx;
  margin-bottom: 20rpx;
}

.preview-content {
  display: flex;
  gap: 20rpx;
  flex-wrap: wrap;
}

.preview-item {
  display: flex;
  flex-direction: column;
  background: rgba(255,255,255,0.2);
  padding: 20rpx 30rpx;
  border-radius: 12rpx;
}

.preview-item .label {
  color: rgba(255,255,255,0.7);
  font-size: 24rpx;
}

.preview-item .value {
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
  margin-top: 8rpx;
}

.quick-amounts {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.quick-amount-btn {
  background: #fff;
  padding: 20rpx 40rpx;
  border-radius: 40rpx;
  font-size: 28rpx;
  color: #333;
}

.form-group {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 30rpx;
  border-radius: 16rpx;
  margin-bottom: 20rpx;
}

.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #333;
}

.input, .picker {
  flex: 1;
  font-size: 32rpx;
  text-align: right;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  text-align: center;
  padding: 30rpx;
  border-radius: 50rpx;
  font-size: 34rpx;
  font-weight: bold;
  margin-top: 40rpx;
}
</style>
