<template>
  <div class="server-search-bar">
    <el-input
      :model-value="nameValue"
      placeholder="按名称搜索"
      clearable
      class="server-search-bar__input"
      @update:model-value="(v: string) => emit('update:nameValue', v ?? '')"
    >
      <template #prefix>
        <svg
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <circle
            cx="11"
            cy="11"
            r="6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          />
          <path
            d="M16 16 L20 20"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          />
        </svg>
      </template>
    </el-input>
    <el-input
      :model-value="hostValue"
      placeholder="按主机地址搜索"
      clearable
      class="server-search-bar__input"
      @update:model-value="(v: string) => emit('update:hostValue', v ?? '')"
    >
      <template #prefix>
        <svg
          viewBox="0 0 24 24"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          <circle
            cx="11"
            cy="11"
            r="6"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          />
          <path
            d="M16 16 L20 20"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
          />
        </svg>
      </template>
    </el-input>
    <el-select
      :model-value="pageSize"
      class="server-search-bar__page-size"
      @change="(v: number) => emit('update:pageSize', v)"
    >
      <el-option
        v-for="opt in pageSizeOptions"
        :key="opt"
        :label="`${opt} 条/页`"
        :value="opt"
      />
    </el-select>
    <el-button @click="emit('reload')">
      刷新
    </el-button>
  </div>
</template>

<script setup lang="ts">
/**
 * ServerListView 工具条:名称搜索 + 主机搜索 + 每页大小 + 刷新。
 *
 * @prop nameValue 名称搜索值
 * @prop hostValue 主机搜索值
 * @prop pageSize 每页大小
 * @prop pageSizeOptions 每页大小选项
 * @event update:nameValue 名称变化
 * @event update:hostValue 主机变化
 * @event update:pageSize 每页变化
 * @event reload 点击刷新
 */

defineProps<{
  nameValue: string
  hostValue: string
  pageSize: number
  pageSizeOptions: number[]
}>()

const emit = defineEmits<{
  (e: 'update:nameValue', value: string): void
  (e: 'update:hostValue', value: string): void
  (e: 'update:pageSize', value: number): void
  (e: 'reload'): void
}>()
</script>

<style scoped>
.server-search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.server-search-bar__input {
  flex: 1;
  min-width: 180px;
  max-width: 280px;
}

.server-search-bar__input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.85);
}

.server-search-bar__input :deep(.el-input__prefix svg) {
  width: 14px;
  height: 14px;
}

.server-search-bar__page-size {
  width: 130px;
}
</style>