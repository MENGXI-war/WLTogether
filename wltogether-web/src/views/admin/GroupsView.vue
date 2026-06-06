<template>
  <div class="admin-table-page">
    <div class="toolbar" v-if="selectedRows.length > 0">
      <el-button type="danger" @click="batchDelete">批量删除 ({{ selectedRows.length }})</el-button>
    </div>
    <el-table ref="tableRef" :data="tableData" v-loading="loading" stripe border style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="name" label="群组名称" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="180">
        <template #default="{ row }">
          {{ row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="ownerId" label="群主ID" width="80" align="center" />
      <el-table-column prop="joinMode" label="加群方式" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="joinModeTag(row.joinMode)" size="small">
            {{ joinModeLabel(row.joinMode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" min-width="120">
        <template #default="{ row }">
          {{ row.tags || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="storageUsedBytes" label="存储用量" width="100" align="right">
        <template #default="{ row }">
          {{ formatBytes(row.storageUsedBytes) }}
        </template>
      </el-table-column>
      <el-table-column prop="lastMessageAt" label="最后消息" width="170">
        <template #default="{ row }">
          {{ formatTime(row.lastMessageAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >
            删除群组
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="() => loadData(1)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const selectedRows = ref([])
function onSelectionChange(rows) { selectedRows.value = rows }

function joinModeTag(mode) {
  const map = { OPEN: 'success', APPROVAL: 'warning', INVITE_ONLY: 'info' }
  return map[mode] || 'info'
}
function joinModeLabel(mode) {
  const map = { OPEN: '开放', APPROVAL: '审批', INVITE_ONLY: '仅邀请' }
  return map[mode] || mode
}
function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}
function formatBytes(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0, v = bytes
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++ }
  return v.toFixed(1) + ' ' + units[i]
}

async function loadData(pg) {
  if (pg) page.value = pg
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value, sort: 'createdAt,desc' }
    const res = await adminApi.getGroups(params)
    tableData.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除群组 "${row.name}" 吗？此操作不可恢复。`, '删除群组', {
      type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await adminApi.deleteGroup(row.id)
    ElMessage.success('群组已删除')
    loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

async function batchDelete() {
  const names = selectedRows.value.map(r => r.name).join('、')
  try {
    await ElMessageBox.confirm(
      `确定批量删除 ${selectedRows.value.length} 个群组（${names}）吗？此操作不可恢复。`,
      '批量删除群组', { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch { return }
  let ok = 0, fail = 0
  await Promise.all(selectedRows.value.map(r =>
    adminApi.deleteGroup(r.id).then(() => ok++).catch(() => fail++)
  ))
  ElMessage.success(`已删除 ${ok} 个${fail > 0 ? `，${fail} 个失败` : ''}`)
  loadData()
}

loadData()
</script>

<style scoped>
.admin-table-page {
  max-width: 100%;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
