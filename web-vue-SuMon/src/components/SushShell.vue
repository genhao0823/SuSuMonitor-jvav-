<template>
  <main class="sush-shell">
    <div class="sush-shell__glass">
      <header class="sush-shell__header">
        <h1 class="sush-shell__title">
          {{ panelTitle }}
        </h1>
        <p class="sush-shell__sub">
          {{ panelSub }}
        </p>
      </header>

      <div class="sush-shell__form">
        <slot />
      </div>

      <footer class="sush-shell__footer">
        <span>{{ footerHint }}</span>
      </footer>
    </div>
  </main>
</template>

<script setup lang="ts">
/**
 * 涂山苏苏认证页面右侧玻璃形态表单容器。
 *
 * - 背景渐变 + 浮动光泽动画(liquid-hue + liquid-shimmer)
 * - 玻璃卡片 + backdrop-filter
 *
 * @prop panelTitle 表单标题
 * @prop panelSub 表单副标
 * @prop footerHint 表单底部 hint
 */

defineProps<{
  panelTitle: string
  panelSub: string
  footerHint: string
}>()
</script>

<style scoped>
.sush-shell {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  isolation: isolate;
  overflow: hidden;
}

.sush-shell::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 20% 15%, rgba(255, 175, 200, 0.55) 0%, transparent 45%),
    radial-gradient(circle at 80% 85%, rgba(245, 215, 130, 0.4) 0%, transparent 45%),
    radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.3) 0%, transparent 60%);
  z-index: 0;
  pointer-events: none;
  animation: sush-shell__liquid-hue 12s ease-in-out infinite;
}

.sush-shell::after {
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
  animation: sush-shell__shimmer 8s linear infinite;
  mix-blend-mode: overlay;
}

@keyframes sush-shell__liquid-hue {
  0%, 100% { transform: translate(0, 0); opacity: 1; }
  33%      { transform: translate(2%, -1%); opacity: 0.85; }
  66%      { transform: translate(-1%, 1%); opacity: 0.92; }
}

@keyframes sush-shell__shimmer {
  0%   { transform: translate(-30%, -30%) rotate(8deg); }
  100% { transform: translate(30%, 30%) rotate(8deg); }
}

.sush-shell__glass {
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

.sush-shell__glass::before {
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

.sush-shell__header {
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 28px;
}

.sush-shell__title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #2a1626;
  letter-spacing: 2px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.6);
}

.sush-shell__sub {
  margin: 0;
  font-size: 13px;
  color: #6d3b54;
  letter-spacing: 1px;
}

.sush-shell__form {
  position: relative;
  z-index: 1;
  width: 100%;
}

.sush-shell__form :deep(.el-form-item__label) {
  color: #2a1626;
  font-weight: 600;
  padding-bottom: 6px;
}

.sush-shell__form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border-radius: 10px;
  padding: 4px 12px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
  transition: box-shadow 0.2s, background 0.2s;
}

.sush-shell__form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.95);
  box-shadow:
    0 0 0 1px #ff5b8a inset,
    0 4px 12px rgba(255, 91, 138, 0.15);
}

.sush-shell__form :deep(.el-input__inner) {
  height: 42px;
  font-size: 14px;
}

.sush-shell__footer {
  position: relative;
  z-index: 1;
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: #8a5872;
  letter-spacing: 1px;
}

@media (max-width: 480px) {
  .sush-shell {
    padding: 24px 16px;
  }

  .sush-shell__glass {
    padding: 28px 22px;
    border-radius: 20px;
  }
}
</style>