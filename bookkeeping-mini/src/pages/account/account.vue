<template>
  <view class="container">
    <!-- 顶部导航 -->
    <view class="top-nav">
      <view class="nav-btn" @click="goBack">‹ 返回</view>
      <view class="nav-title">账户管理</view>
      <view class="nav-btn" @click="goHome">🏠</view>
    </view>

    <!-- 账户列表 -->
    <view class="account-list">
      <view v-if="loading" class="loading">加载中...</view>
      <view v-else-if="accountList.length === 0" class="empty">
        暂无账户，快去添加吧！
      </view>

      <view
        v-for="item in accountList"
        :key="item.id"
        class="account-item"
        :class="{ active: currentAccount?.id === item.id }"
        @click="selectAccount(item)"
      >
        <view class="account-icon" :style="{ backgroundColor: item.color }">
          {{ item.icon || '💰' }}
        </view>
        <view class="account-info">
          <text class="account-name">{{ item.name }}</text>
          <text v-if="item.isDefault" class="default-tag">默认</text>
        </view>
        <view class="account-balance">¥{{ item.balance.toFixed(2) }}</view>
        <view class="delete-btn" @click.stop="handleDelete(item.id)">删除</view>
      </view>
    </view>

    <!-- 添加账户按钮 -->
    <view class="add-btn" @click="showAddModal = true">
      + 添加账户
    </view>

    <!-- 添加账户弹窗 -->
    <view v-if="showAddModal" class="modal-mask" @click="showAddModal = false">
      <view class="modal" @click.stop>
        <view class="modal-title">添加账户</view>

        <view class="form-group">
          <text class="label">账户名称</text>
          <input v-model="form.name" class="input" placeholder="如：现金、银行卡" />
        </view>

        <view class="form-group">
          <text class="label">账户类型</text>
          <picker :range="accountTypes" range-key="label" @change="onTypeChange">
            <view class="picker">
              {{ currentType.label }}
            </view>
          </picker>
        </view>

        <view class="form-group">
          <text class="label">初始余额</text>
          <input v-model="form.balance" type="digit" class="input" placeholder="当前余额" />
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
import { ref, onMounted } from 'vue';
import { getAccountList, createAccount, deleteAccount, getDefaultAccount } from '@/api/account';
import { Account } from '@/types';

const loading = ref(false);
const accountList = ref<Account[]>([]);
const currentAccount = ref<Account | null>(null);
const showAddModal = ref(false);
const form = ref({ name: '', balance: '', type: 'cash' });

const goBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
  } else {
    uni.switchTab({ url: '/pages/index/index' });
  }
};
const goHome = () => uni.switchTab({ url: '/pages/index/index' });

const accountTypes = [
  { label: '现金', value: 'cash' },
  { label: '银行卡', value: 'bank' },
  { label: '支付宝', value: 'alipay' },
  { label: '微信', value: 'wechat' },
  { label: '信用卡', value: 'credit' },
  { label: '其他', value: 'other' }
];

const typeIcons: Record<string, string> = {
  cash: '💵',
  bank: '🏦',
  alipay: '💳',
  wechat: '💬',
  credit: '💳',
  other: '💰'
};

const typeColors: Record<string, string> = {
  cash: '#52c41a',
  bank: '#1890ff',
  alipay: '#1890ff',
  wechat: '#07c160',
  credit: '#fa8c16',
  other: '#999999'
};

const currentType = ref(accountTypes[0]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getAccountList();
    if (res.code === 200) {
      accountList.value = res.data || [];
    }

    // 获取默认账户
    const defaultRes = await getDefaultAccount();
    if (defaultRes.code === 200) {
      currentAccount.value = defaultRes.data;
    }
  } catch (error) {
    console.error('加载账户失败:', error);
  } finally {
    loading.value = false;
  }
};

const onTypeChange = (e: any) => {
  currentType.value = accountTypes[e.detail.value];
  form.value.type = currentType.value.value;
};

const handleSubmit = async () => {
  if (!form.value.name) {
    uni.showToast({ title: '请输入账户名称', icon: 'none' });
    return;
  }

  try {
    const data = {
      name: form.value.name,
      type: form.value.type,
      balance: parseFloat(form.value.balance) || 0,
      icon: typeIcons[form.value.type],
      color: typeColors[form.value.type]
    };

    const res = await createAccount(data);
    if (res.code === 200) {
      uni.showToast({ title: '添加成功', icon: 'success' });
      showAddModal.value = false;
      form.value = { name: '', balance: '', type: 'cash' };
      loadData();
    }
  } catch (error) {
    console.error('添加账户失败:', error);
    uni.showToast({ title: '添加失败', icon: 'none' });
  }
};

const selectAccount = (account: Account) => {
  currentAccount.value = account;
  uni.setStorageSync('currentAccountId', account.id);
  uni.showToast({ title: `已切换到${account.name}`, icon: 'success' });
};

const handleDelete = (id: number) => {
  uni.showModal({
    title: '确认删除',
    content: '确定要删除这个账户吗？',
    success: async (res) => {
      if (res.confirm) {
        const r = await deleteAccount(id);
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

.account-list {
  margin-bottom: 20rpx;
}

.empty, .loading {
  text-align: center;
  color: #999;
  padding: 100rpx 0;
  font-size: 28rpx;
}

.account-item {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;
}

.account-item.active {
  border: 2rpx solid #667eea;
}

.account-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  margin-right: 20rpx;
}

.account-info {
  flex: 1;
  display: flex;
  align-items: center;
}

.account-name {
  font-size: 30rpx;
  color: #333;
}

.default-tag {
  font-size: 20rpx;
  color: #667eea;
  background: #f0f0ff;
  padding: 4rpx 12rpx;
  border-radius: 8rpx;
  margin-left: 15rpx;
}

.account-balance {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.delete-btn {
  font-size: 24rpx;
  color: #ff4d4f;
  padding: 10rpx 20rpx;
  margin-left: 15rpx;
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
