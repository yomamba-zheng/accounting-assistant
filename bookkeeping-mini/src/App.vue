<template>
  <router-view />
</template>

<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app';

onLaunch(() => {
  console.log('App Launch');
});
</script>

<style>
/* 全局样式 */
page {
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}
.container {
  padding: 20rpx;
}
.btn-primary {
  background-color: #007aff;
  color: white;
  border-radius: 8rpx;
  padding: 20rpx;
  text-align: center;
  margin-top: 30rpx;
}
</style>