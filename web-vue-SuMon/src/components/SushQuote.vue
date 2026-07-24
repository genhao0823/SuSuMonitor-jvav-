<template>
  <div
    class="sush-quote"
    role="button"
    tabindex="0"
    :aria-label="`签名引言,共 ${stageQuotes.length} 句,点击换一句。当前第 ${quoteIndex + 1} 句`"
    @click="advanceQuote"
    @keydown.enter.prevent="advanceQuote"
    @keydown.space.prevent="advanceQuote"
  >
    <div class="sush-quote__text-wrap">
      <transition name="quote-fade">
        <p
          :key="quoteIndex"
          class="sush-quote__text"
        >
          {{ stageQuotes[quoteIndex] }}
        </p>
      </transition>
    </div>
    <p class="sush-quote__author">
      — 涂山 苏苏
    </p>
    <span class="sush-quote__hint">
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
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'

/**
 * 涂山苏苏签名引言:可点击切换 + 键盘 Enter/Space 切换。
 *
 * @prop stageQuotes 引言池
 * @event click 点击切换
 * @event keydown.enter 键盘 Enter 切换
 * @event keydown.space 键盘 Space 切换
 */

const props = defineProps<{
  stageQuotes: string[]
}>()

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

onMounted(() => {
  if (props.stageQuotes.length > 0) {
    quoteIndex.value = Math.floor(Math.random() * props.stageQuotes.length)
  }
})
</script>

<style scoped>
.sush-quote {
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

.sush-quote:hover,
.sush-quote:focus-visible {
  background: rgba(255, 255, 255, 0.45);
  border-color: rgba(255, 91, 138, 0.35);
  transform: translateY(-1px);
}

.sush-quote:active {
  transform: translateY(0);
  background: rgba(255, 255, 255, 0.6);
}

.sush-quote__text-wrap {
  position: relative;
}

.sush-quote__text {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  font-style: italic;
  color: #2a1626;
  transition: color 0.2s ease;
}

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

.sush-quote:hover .sush-quote__text,
.sush-quote:focus-visible .sush-quote__text {
  color: #b7325c;
}

.sush-quote__author {
  margin: 6px 0 0;
  font-size: 11px;
  opacity: 0.7;
  letter-spacing: 1px;
}

.sush-quote__hint {
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

.sush-quote:hover .sush-quote__hint,
.sush-quote:focus-visible .sush-quote__hint {
  opacity: 0.85;
}

.sush-quote__hint svg {
  width: 12px;
  height: 12px;
}
</style>