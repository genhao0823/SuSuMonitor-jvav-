<template>
  <img
    :src="currentSrc"
    :alt="alt"
    class="tushan-fox-mark"
    :class="{ 'tushan-fox-mark--rounded': rounded }"
    :style="sizeStyle"
    :width="size"
    :height="size"
    @error="onError"
  >
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * 涂山苏苏头像组件。
 * 默认从 OSS 远程 URL 加载,加载失败时降级到本地 public/tushansusu-hero.jpg。
 * 使用圆形剪裁与简单淡入浮动动画,适合作为卡片头像/角标。
 *
 * @prop src 默认远程 URL,可覆盖为其他来源
 * @prop fallback 加载失败兜底路径
 * @prop size 边长像素,默认 28
 * @prop alt alt 文本,默认 "涂山苏苏"
 * @prop rounded true=圆形剪裁,false=圆角方形
 */
const props = withDefaults(
  defineProps<{
    src?: string
    fallback?: string
    size?: number
    alt?: string
    rounded?: boolean
  }>(),
  {
    src: 'https://java-ai-genhaosan.oss-cn-beijing.aliyuncs.com/0dc3f6ad-d7df-4e11-a388-8c5f79804c89.jpg',
    fallback: '/tushansusu-hero.jpg',
    size: 28,
    alt: '涂山苏苏',
    rounded: true
  }
)

const failed = ref(false)

/**
 * 当前实际渲染的图片 URL。远程加载失败时降级到 fallback。
 */
const currentSrc = computed<string>(() => {
  if (failed.value) {
    return props.fallback
  }
  return props.src
})

/**
 * 处理远程图片加载失败,标记降级状态。
 */
function onError(): void {
  failed.value = true
}

const sizeStyle = computed(() => ({
  width: `${props.size}px`,
  height: `${props.size}px`
}))
</script>

<style scoped>
.tushan-fox-mark {
  display: block;
  object-fit: cover;
  flex-shrink: 0;
  border: 1.5px solid rgba(255, 255, 255, 0.7);
  box-shadow: 0 2px 6px rgba(183, 50, 92, 0.18);
  animation: tushan-fox-bob 4s ease-in-out infinite;
}

.tushan-fox-mark--rounded {
  border-radius: 50%;
}

@keyframes tushan-fox-bob {
  0%, 100% { transform: translateY(0); }
  50%      { transform: translateY(-2px); }
}
</style>