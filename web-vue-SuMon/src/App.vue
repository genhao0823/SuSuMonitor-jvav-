<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'

/**
 * 应用根组件。
 *
 * 通过 Element Plus ConfigProvider 提供中文语言环境,
 * 所有页面通过 router-view 渲染。
 */
</script>

<style>
html,
body,
#app {
  height: 100%;
  margin: 0;
  padding: 0;
}

/* NProgress 路由切换进度条主题:沿用全站玫红渐变 */
#nprogress .bar {
  background: linear-gradient(90deg, #ff5b8a, #b7325c) !important;
  height: 3px !important;
}
#nprogress .peg {
  box-shadow: 0 0 10px #ff5b8a, 0 0 5px #b7325c !important;
}
#nprogress .spinner {
  display: none !important;
}
</style>
