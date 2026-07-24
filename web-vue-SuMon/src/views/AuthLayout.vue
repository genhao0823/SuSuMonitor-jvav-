<template>
  <div class="auth-view">
    <aside
      class="auth-view__stage"
      :class="{ 'auth-view__stage--wild': isWildActive }"
    >
      <img
        v-if="effectiveHeroImage"
        :src="effectiveHeroImage"
        :alt="heroAlt"
        class="auth-view__stage-img"
        @error="onHeroImageError"
      >
      <div class="auth-view__veil" />

      <svg
        class="auth-view__pattern auth-view__pattern--top"
        viewBox="0 0 320 80"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M0 60 Q40 30 80 60 T160 60 T240 60 T320 60"
          fill="none"
          stroke="#ffd0d8"
          stroke-width="2"
          stroke-linecap="round"
          opacity="0.55"
        />
        <path
          d="M0 70 Q40 50 80 70 T160 70 T240 70 T320 70"
          fill="none"
          stroke="#ffb6c1"
          stroke-width="1.5"
          stroke-linecap="round"
          opacity="0.45"
        />
      </svg>

      <svg
        class="auth-view__pattern auth-view__pattern--bottom"
        viewBox="0 0 320 80"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M0 20 Q40 -10 80 20 T160 20 T240 20 T320 20"
          fill="none"
          stroke="#ffd0d8"
          stroke-width="2"
          stroke-linecap="round"
          opacity="0.55"
        />
      </svg>

      <svg
        class="auth-view__bells auth-view__bells--left"
        viewBox="0 0 64 80"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M32 6 C28 6 26 10 26 16 L26 44 L18 50 L46 50 L38 44 L38 16 C38 10 36 6 32 6 Z"
          fill="#f5b942"
          stroke="#b7325c"
          stroke-width="1.5"
        />
        <circle
          cx="32"
          cy="58"
          r="4"
          fill="#b7325c"
        />
        <path
          d="M32 56 L32 70"
          stroke="#b7325c"
          stroke-width="1.5"
        />
      </svg>

      <svg
        class="auth-view__bells auth-view__bells--right"
        viewBox="0 0 64 80"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <path
          d="M32 6 C28 6 26 10 26 16 L26 44 L18 50 L46 50 L38 44 L38 16 C38 10 36 6 32 6 Z"
          fill="#f5b942"
          stroke="#b7325c"
          stroke-width="1.5"
        />
        <circle
          cx="32"
          cy="58"
          r="4"
          fill="#b7325c"
        />
        <path
          d="M32 56 L32 70"
          stroke="#b7325c"
          stroke-width="1.5"
        />
      </svg>

      <SushFallingPetals />

      <div class="auth-view__brand-block">
        <div class="auth-view__brand-title">
          <span class="auth-view__brand-mark">Su</span><span class="auth-view__brand-mark auth-view__brand-mark--accent">Su</span><span class="auth-view__brand-suffix">Monitor</span>
        </div>
        <div class="auth-view__brand-tagline">
          {{ stageTagline }}
        </div>
        <div class="auth-view__brand-meta">
          SuSu · 与运维一同成长
        </div>
      </div>

      <SushQuote :stage-quotes="stageQuotes" />
    </aside>

    <SushShell
      :panel-title="panelTitle"
      :panel-sub="panelSub"
      :footer-hint="footerHint"
    >
      <slot />
    </SushShell>
  </div>
</template>

<script setup lang="ts">
/**
 * 认证页面共享布局。
 *
 * 左侧:涂山苏苏主题装饰舞台(波浪线、铃铛、品牌标题 + 飘落花瓣 + 可点击切换的签名引言)。
 * 右侧:聚焦的玻璃形态表单卡片(由 SushShell 提供)。
 *
 * 子组件:
 * - SushFallingPetals: 飘落花瓣动画
 * - SushQuote: 可点击切换的签名引言
 * - SushShell: 右侧玻璃表单容器
 *
 * 父组件只需提供 slot 内的表单字段与提交逻辑。
 */

import SushFallingPetals from '@/components/SushFallingPetals.vue'
import SushQuote from '@/components/SushQuote.vue'
import SushShell from '@/components/SushShell.vue'
import { computed, ref } from 'vue'

/**
 * 控制全屏装饰联动(铃铛/花瓣/品牌/波浪)。
 * 用户希望整页停留期间持续展示 wild 动效,因此设为常量 true:
 * 铃铛永远狂摇、花瓣永远雪崩、品牌永远脉冲、波浪永远亮起。
 * 离开该页(Dashboard/Forbidden)组件销毁后,所有 CSS 动画自然停止。
 */
const isWildActive = true

const props = withDefaults(
  defineProps<{
    stageTagline: string
    stageQuotes: string[]
    panelTitle: string
    panelSub: string
    footerHint: string
    heroImage?: string | null
    heroImageFallback?: string | null
    heroAlt?: string
  }>(),
  {
    heroImage: null,
    heroImageFallback: null,
    heroAlt: '涂山苏苏'
  }
)

const heroImageFailed = ref(false)

/**
 * 当前实际渲染的图片 URL:
 * 1. 优先 heroImage
 * 2. 失败或缺失时降级 heroImageFallback
 * 3. 都缺失返回 null,模板不渲染 <img>
 */
const effectiveHeroImage = computed<string | null>(() => {
  if (props.heroImage && !heroImageFailed.value) {
    return props.heroImage
  }
  if (props.heroImageFallback) {
    return props.heroImageFallback
  }
  return null
})

function onHeroImageError(): void {
  heroImageFailed.value = true
}
</script>

<style scoped>
.auth-view {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(380px, 1.05fr);
  min-height: 100vh;
  background:
    radial-gradient(circle at 25% 30%, #ffd0e0 0%, transparent 45%),
    radial-gradient(circle at 75% 70%, #f5d8a4 0%, transparent 45%),
    linear-gradient(135deg, #ffeaf1 0%, #ffd0d8 50%, #f9b8c8 100%);
}

.auth-view__stage {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
}

.auth-view__stage-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 25%;
  z-index: 0;
  display: block;
}

.auth-view__veil {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    155deg,
    rgba(255, 232, 239, 0.05) 0%,
    rgba(183, 50, 92, 0.15) 60%,
    rgba(45, 18, 36, 0.25) 100%
  );
  z-index: 1;
  pointer-events: none;
}

.auth-view__pattern {
  position: absolute;
  left: 0;
  right: 0;
  width: 100%;
  height: 80px;
  z-index: 2;
  pointer-events: none;
}

.auth-view__pattern--top {
  top: 0;
}

.auth-view__pattern--bottom {
  bottom: 0;
  transform: scaleY(-1);
}

.auth-view__bells {
  position: absolute;
  width: 56px;
  height: 70px;
  z-index: 3;
  filter: drop-shadow(0 6px 12px rgba(183, 50, 92, 0.25));
  animation: bells-sway 4s ease-in-out infinite;
}

.auth-view__bells--left {
  top: 16%;
  left: 12%;
}

.auth-view__bells--right {
  top: 64%;
  right: 12%;
  animation-delay: 2s;
}

@keyframes bells-sway {
  0%, 100% { transform: rotate(-4deg); }
  50%      { transform: rotate(4deg); }
}

.auth-view__brand-block {
  position: absolute;
  bottom: 180px;
  left: 0;
  right: 0;
  z-index: 6;
  text-align: center;
  color: #2a1626;
  padding: 0 24px;
}

.auth-view__brand-title {
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 4px;
  margin-bottom: 8px;
  text-shadow: 0 2px 8px rgba(255, 255, 255, 0.6);
}

.auth-view__brand-mark {
  color: #b7325c;
}

.auth-view__brand-mark--accent {
  color: #f5b942;
}

.auth-view__brand-suffix {
  color: #6d3b54;
  font-size: 22px;
  letter-spacing: 6px;
  margin-left: 6px;
  font-weight: 500;
}

.auth-view__brand-tagline {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 5px;
  margin-bottom: 4px;
  color: #b7325c;
}

.auth-view__brand-meta {
  font-size: 12px;
  letter-spacing: 2px;
  opacity: 0.7;
  color: #6d3b54;
}

/* ---------- wild 联动动效 ----------
 * `.auth-view__stage--wild` 类挂载于容器,触发以下同步增强:
 * - 铃铛摆动 ±14°,周期 1.2s
 * - 花瓣下落加速到 6s
 * - 品牌标题慢脉冲 1.04x,周期 3s
 * - 顶部/底部波浪线 opacity 拉到 1
 */
.auth-view__stage--wild .auth-view__bells {
  animation: bells-sway-wild 1.2s ease-in-out infinite;
}

.auth-view__stage--wild .auth-view__bells--right {
  animation-delay: 0.6s;
}

.auth-view__stage--wild :deep(.sush-falling-petals__petal) {
  animation-duration: 6s;
}

.auth-view__stage--wild .auth-view__brand-title {
  animation: brand-pulse 3s ease-in-out infinite;
}

.auth-view__stage--wild .auth-view__pattern {
  opacity: 1;
}

@keyframes bells-sway-wild {
  0%, 100% { transform: rotate(-14deg); }
  50%      { transform: rotate(14deg); }
}

@keyframes brand-pulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.04); }
}

@media (max-width: 960px) {
  .auth-view {
    grid-template-columns: 1fr;
    grid-template-rows: 260px auto;
  }

  .auth-view__stage {
    height: 260px;
  }

  .auth-view__brand-block {
    bottom: 100px;
  }

  .auth-view__brand-title {
    font-size: 28px;
  }

  .auth-view__brand-suffix {
    font-size: 16px;
  }

  .auth-view__brand-tagline {
    font-size: 12px;
    letter-spacing: 3px;
  }

  .auth-view__bells {
    width: 36px;
    height: 46px;
  }

  .auth-view__bells--left {
    top: 8%;
  }

  .auth-view__bells--right {
    top: 50%;
  }
}
</style>