<template>
  <div class="server-pagination">
    <el-pagination
      :current-page="page"
      :page-size="pageSize"
      :total="total"
      :page-sizes="pageSizeOptions"
      layout="total, prev, pager, next, jumper"
      background
      @current-change="(v: number) => emit('update:page', v)"
      @size-change="(v: number) => emit('update:pageSize', v)"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * ServerListView 分页器。
 *
 * @prop page 当前页
 * @prop pageSize 每页大小
 * @prop total 总数
 * @prop pageSizeOptions 每页大小选项
 * @event update:page 页码变化
 * @event update:pageSize 每页大小变化
 */

defineProps<{
  page: number
  pageSize: number
  total: number
  pageSizeOptions: number[]
}>()

const emit = defineEmits<{
  (e: 'update:page', value: number): void
  (e: 'update:pageSize', value: number): void
}>()
</script>

<style scoped>
.server-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>