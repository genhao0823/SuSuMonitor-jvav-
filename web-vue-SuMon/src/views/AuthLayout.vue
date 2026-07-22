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

      <div class="auth-view__sakura">
        <span
          v-for="(p, i) in sakura"
          :key="i"
          :style="p"
          class="auth-view__sakura-petal"
        />
      </div>

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

      <div
        class="auth-view__quote"
        role="button"
        tabindex="0"
        :aria-label="`签名引言,共 ${stageQuotes.length} 句,点击换一句。当前第 ${quoteIndex + 1} 句`"
        @click="advanceQuote"
        @keydown.enter.prevent="advanceQuote"
        @keydown.space.prevent="advanceQuote"
      >
        <div class="auth-view__quote-text-wrap">
          <transition name="quote-fade">
            <p
              :key="quoteIndex"
              class="auth-view__quote-text"
            >
              {{ stageQuotes[quoteIndex] }}
            </p>
          </transition>
        </div>
        <p class="auth-view__quote-author">
          — 涂山 苏苏
        </p>
        <span class="auth-view__quote-hint">
          <svg
            viewBox="0 0 16 16"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <path
              d="M8 3 A5 5 0 1 1 3 8"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
            />
            <path
              d="M3 4 L3 8 L7 8"
              fill="none"
              stroke="currentColor"
              stroke-width="1.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <span>点一下,换一句</span>
        </span>
      </div>
    </aside>

    <main class="auth-view__panel">
      <div class="auth-view__panel-glass">
        <header class="auth-view__panel-header">
          <h1 class="auth-view__panel-title">
            {{ panelTitle }}
          </h1>
          <p class="auth-view__panel-sub">
            {{ panelSub }}
          </p>
        </header>

        <div class="auth-view__form">
          <slot />
        </div>

        <footer class="auth-view__panel-footer">
          <span>{{ footerHint }}</span>
        </footer>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
// 注:`isWildActive` 现在是常量 true,持续展示 wild 联动;无需 ref/onUnmounted。

/**
 * 认证页面共享布局。
 *
 * 左侧:涂山苏苏主题装饰舞台(波浪线、铃铛、品牌标题、可点击切换的签名引言),
 * 可选背景图(heroImage)叠加在最底层,加载失败时降级到 heroImageFallback。
 * 右侧:聚焦的玻璃形态表单卡片(标题/副标 + slot 注入的表单 + 底部 hint)。
 *
 * 子组件只需提供 slot 内的表单字段与提交逻辑,所有装饰与样式由本组件统一管理。
 *
 * @prop stageTagline 左侧标语文本
 * @prop stageQuotes 引言池,每次进入页面随机抽一句展示,点击或按 Enter/Space 切换到下一句
 * @prop panelTitle 表单标题
 * @prop panelSub 表单副标
 * @prop footerHint 表单底部 hint
 * @prop heroImage 可选。左侧背景图 URL;为 null/undefined 时不显示图片(纯装饰)。
 * @prop heroImageFallback 可选。heroImage 加载失败时降级的本地路径。
 * @prop heroAlt 图片 alt 文本,默认 "涂山苏苏"
 */
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

/**
 * 当前展示的引言索引。onMounted 随机抽初值,点击/键盘事件后由 advanceQuote 推进。
 */
const quoteIndex = ref(0)

/**
 * 推进到下一句,索引到尾后回 0。
 * 由点击、Enter、Space 三种交互触发。
 */
function advanceQuote(): void {
  if (props.stageQuotes.length === 0) {
    return
  }
  quoteIndex.value = (quoteIndex.value + 1) % props.stageQuotes.length
}

/**
 * 控制全屏装饰联动(铃铛/花瓣/品牌/波浪)。
 * 用户希望整页停留期间持续展示 wild 动效,因此设为常量 true:
 * 铃铛永远狂摇、花瓣永远雪崩、品牌永远脉冲、波浪永远亮起。
 * 离开该页(Dashboard/Forbidden)组件销毁后,所有 CSS 动画自然停止。
 */
const isWildActive = true

const heroImageFailed = ref(false)

/**
 * 当前实际渲染的图片 URL。逻辑:
 * 1. 优先使用 heroImage;
 * 2. heroImage 缺失或加载失败时,降级到 heroImageFallback;
 * 3. 都缺失时返回 null,模板不渲染 <img>。
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

/**
 * 处理 heroImage 加载失败,标记错误状态以便降级到 fallback。
 */
function onHeroImageError(): void {
  heroImageFailed.value = true
}

onMounted(() => {
  if (props.stageQuotes.length > 0) {
    quoteIndex.value = Math.floor(Math.random() * props.stageQuotes.length)
  }
})

/**
 * 飘落花瓣:18 朵,延迟 0-8.5s 均匀错开,时长 10-13s,
 * 让画面任何时刻同时有 6-9 朵在下落,呈现连续不断的飘落流。
 * 字段顺序与动画 CSS 变量对应: --s(sizes)/ --d(delay)/ --t(duration)
 */
const sakura = [
  { top: '5%',  left: '12%', size: 14, delay: '0s',   duration: '11s' },
  { top: '8%',  left: '36%', size: 10, delay: '0.5s', duration: '12s' },
  { top: '4%',  left: '58%', size: 16, delay: '1.0s', duration: '10s' },
  { top: '12%', left: '78%', size: 12, delay: '1.5s', duration: '13s' },
  { top: '20%', left: '24%', size: 9,  delay: '2.0s', duration: '11s' },
  { top: '18%', left: '90%', size: 14, delay: '2.5s', duration: '12s' },
  { top: '32%', left: '4%',  size: 16, delay: '3.0s', duration: '10s' },
  { top: '28%', left: '46%', size: 8,  delay: '3.5s', duration: '13s' },
  { top: '36%', left: '68%', size: 12, delay: '4.0s', duration: '11s' },
  { top: '44%', left: '14%', size: 10, delay: '4.5s', duration: '12s' },
  { top: '48%', left: '82%', size: 14, delay: '5.0s', duration: '10s' },
  { top: '52%', left: '36%', size: 9,  delay: '5.5s', duration: '13s' },
  { top: '58%', left: '60%', size: 16, delay: '6.0s', duration: '11s' },
  { top: '64%', left: '8%',  size: 12, delay: '6.5s', duration: '12s' },
  { top: '70%', left: '88%', size: 14, delay: '7.0s', duration: '10s' },
  { top: '76%', left: '28%', size: 10, delay: '7.5s', duration: '13s' },
  { top: '82%', left: '52%', size: 16, delay: '8.0s', duration: '11s' },
  { top: '88%', left: '74%', size: 12, delay: '8.5s', duration: '12s' }
] 
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

.auth-view__sakura {
  position: absolute;
  inset: 0;
  z-index: 4;
  pointer-events: none;
  overflow: hidden;
}

.auth-view__sakura-petal {
  position: absolute;
  width: var(--s);
  height: var(--s);
  background: radial-gradient(circle at 30% 30%, #fff7fa 0%, #ffd0d8 60%, #ffb6c1 100%);
  border-radius: 60% 40% 60% 40%;
  opacity: 0.85;
  animation: sakura-fall linear infinite;
  --s: 12px;
  animation-delay: var(--d);
  animation-duration: var(--t);
}

.auth-view__sakura-petal:nth-child(odd) {
  background: radial-gradient(circle at 30% 30%, #fff 0%, #ff7aa3 60%, #b7325c 100%);
}

@keyframes sakura-fall {
  0%   { transform: translateY(-20px) rotate(0deg); opacity: 0; }
  10%  { opacity: 0.85; }
  90%  { opacity: 0.85; }
  100% { transform: translateY(120vh) rotate(360deg); opacity: 0; }
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

/* 签名引言:可点击切换 */
.auth-view__quote {
  position: absolute;
  bottom: 32px;
  left: 32px;
  right: 32px;
  z-index: 6;
  text-align: center;
  color: #2a1626;
  cursor: pointer;
  user-select: none;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.0);
  border: 1px dashed transparent;
  transition: background 0.2s ease, border-color 0.2s ease, transform 0.2s ease;
  outline: none;
}

.auth-view__quote:hover,
.auth-view__quote:focus-visible {
  background: rgba(255, 255, 255, 0.45);
  border-color: rgba(255, 91, 138, 0.35);
  transform: translateY(-1px);
}

.auth-view__quote:active {
  transform: translateY(0);
  background: rgba(255, 255, 255, 0.6);
}

/* 签名引言:可点击切换 */
.auth-view__quote-text-wrap {
  position: relative;
}

.auth-view__quote-text {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  font-style: italic;
  color: #2a1626;
  transition: color 0.2s ease;
}

/* crossfade:旧引言淡出上移,新引言淡入下移,中间重叠 250ms */
.quote-fade-enter-active,
.quote-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.quote-fade-leave-active {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
}

.quote-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.quote-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.auth-view__quote:hover .auth-view__quote-text,
.auth-view__quote:focus-visible .auth-view__quote-text {
  color: #b7325c;
}

.auth-view__quote-author {
  margin: 6px 0 0;
  font-size: 11px;
  opacity: 0.7;
  letter-spacing: 1px;
}

.auth-view__quote-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 10px;
  letter-spacing: 1px;
  color: #b7325c;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.auth-view__quote:hover .auth-view__quote-hint,
.auth-view__quote:focus-visible .auth-view__quote-hint {
  opacity: 0.85;
}

.auth-view__quote-hint svg {
  width: 12px;
  height: 12px;
}

/* ---------- 右侧表单面板 ---------- */
.auth-view__panel {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  isolation: isolate;
  overflow: hidden;
}

.auth-view__panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 15%, rgba(255, 175, 200, 0.55) 0%, transparent 45%),
    radial-gradient(circle at 80% 85%, rgba(245, 215, 130, 0.4) 0%, transparent 45%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.3) 0%, transparent 60%);
  z-index: 0;
  pointer-events: none;
  animation: liquid-hue 12s ease-in-out infinite;
}

.auth-view__panel::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    115deg,
    transparent 35%,
    rgba(255, 255, 255, 0.5) 50%,
    transparent 65%
  );
  z-index: 1;
  pointer-events: none;
  animation: liquid-shimmer 8s linear infinite;
  mix-blend-mode: overlay;
}

@keyframes liquid-hue {
  0%, 100% { transform: translate(0, 0); opacity: 1; }
  33%      { transform: translate(2%, -1%); opacity: 0.85; }
  66%      { transform: translate(-1%, 1%); opacity: 0.92; }
}

@keyframes liquid-shimmer {
  0%   { transform: translate(-30%, -30%) rotate(8deg); }
  100% { transform: translate(30%, 30%) rotate(8deg); }
}

.auth-view__panel-glass {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 420px;
  padding: 36px 32px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.42);
  backdrop-filter: blur(40px) saturate(180%);
  -webkit-backdrop-filter: blur(40px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.65);
  box-shadow:
    0 24px 60px rgba(183, 50, 92, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.85),
    inset 0 -1px 0 rgba(183, 50, 92, 0.08);
}

.auth-view__panel-glass::before {
  content: '';
  position: absolute;
  top: 0;
  left: 24px;
  right: 24px;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.95) 50%,
    transparent 100%
  );
  pointer-events: none;
}

.auth-view__panel-header {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 28px;
}

.auth-view__panel-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #2a1626;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.6);
}

.auth-view__panel-sub {
  margin: 0;
  font-size: 13px;
  color: #6d3b54;
  letter-spacing: 1px;
}

.auth-view__form {
  position: relative;
  z-index: 1;
  width: 100%;
}

.auth-view__form :deep(.el-form-item__label) {
  color: #2a1626;
  font-weight: 600;
  padding-bottom: 6px;
}

.auth-view__form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  transition: box-shadow 0.2s, background 0.2s;
}

.auth-view__form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 0 0 1px #ff5b8a inset,
    0 4px 12px rgba(255, 91, 138, 0.15);
}

.auth-view__form :deep(.el-input__inner) {
  height: 42px;
  font-size: 14px;
}

.auth-view__panel-footer {
  position: relative;
  z-index: 1;
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #8a5872;
  letter-spacing: 1px;
}

/* ---------- wild 联动动效 ----------
 * `.auth-view__stage--wild` 类挂载于容器,触发以下同步增强:
 * - 铃铛摆动 ±14°,周期 1.2s
 * - 花瓣下落加速到 6s
 * - 品牌标题慢脉冲 1.04x,周期 3s
 * - 顶部/底部波浪线 opacity 拉到 1
 *
 * 类由 JS 在 onMounted 后 5 秒自动移除;hover/focus 引言会再触发一次 3 秒。 */
.auth-view__stage--wild .auth-view__bells {
  animation: bells-sway-wild 1.2s ease-in-out infinite;
}

.auth-view__stage--wild .auth-view__bells--right {
  animation-delay: 0.6s;
}

.auth-view__stage--wild .auth-view__sakura-petal {
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

  .auth-view__quote-text {
    font-size: 12px;
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

  .auth-view__quote {
    bottom: 12px;
    left: 12px;
    right: 12px;
    padding: 8px 10px;
  }
}

@media (max-width: 480px) {
  .auth-view__panel {
    padding: 24px 16px;
  }

  .auth-view__panel-glass {
    padding: 28px 22px;
    border-radius: 20px;
  }
}
</style>