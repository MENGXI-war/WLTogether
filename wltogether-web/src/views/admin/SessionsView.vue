<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="loadData(1)" style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="已创建" value="CREATED" />
        <el-option label="进行中" value="ACTIVE" />
        <el-option label="已结束" value="ENDED" />
      </el-select>
      <el-button v-if="selectedRows.length > 0" type="danger" @click="batchForceEnd">
        批量强制结束 ({{ selectedRows.length }})
      </el-button>
    </div>

    <el-table ref="tableRef" :data="tableData" v-loading="loading" stripe border style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="groupId" label="所属群组ID" width="100" align="center" />
      <el-table-column prop="hostId" label="主持人ID" width="90" align="center" />
      <el-table-column prop="mediaType" label="媒体类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.mediaType === 'MUSIC' ? 'success' : ''" size="small">
            {{ row.mediaType === 'MUSIC' ? '音乐' : '视频' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="文件大小" width="100" align="right">
        <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="durationSeconds" label="时长" width="80" align="center">
        <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startedAt" label="开始时间" width="170">
        <template #default="{ row }">{{ formatTime(row.startedAt) }}</template>
      </el-table-column>
      <el-table-column prop="endedAt" label="结束时间" width="170">
        <template #default="{ row }">{{ formatTime(row.endedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'CREATED' || row.status === 'ACTIVE'"
            type="danger" size="small" link @click="handleForceEnd(row)"
          >强制结束</el-button>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page" v-model:page-size="size" :total="total"
        :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData" @size-change="() => loadData(1)"
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
const statusFilter = ref('')
const selectedRows = ref([])
function onSelectionChange(rows) { selectedRows.value = rows }

function statusTagType(s) {
  const map = { CREATED: 'warning', ACTIVE: 'success', PAUSED: 'info', ENDED: 'info' }
  return map[s] || 'info'
}
function statusLabel(s) {
  const map = { CREATED: '已创建', ACTIVE: '进行中', PAUSED: '已暂停', ENDED: '已结束' }
  return map[s] || s
}
function formatTime(i) { return i ? new Date(i).toLocaleString('zh-CN') : '-' }
function formatBytes(b) {
  if (!b) return '0 B'
  const u = ['B','KB','MB','GB','TB']; let i = 0, v = b
  while (v >= 1024 && i < u.length - 1) { v /= 1024; i++ }
  return v.toFixed(1) + ' ' + u[i]
}
function formatDuration(s) {
  if (!s) return '-'
  const m = Math.floor(s / 60), sec = s % 60
  return m >= 60 ? `${Math.floor(m/60)}:${String(m%60).padStart(2,'0')}:${String(sec).padStart(2,'0')}` : `${m}:${String(sec).padStart(2,'0')}`
}

async function loadData(pg) {
  if (pg) page.value = pg
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value, sort: 'createdAt,desc' }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await adminApi.getSessions(params)
    tableData.value = res.content; total.value = res.totalElements
  } finally { loading.value = false }
}

async function handleForceEnd(row) {
  try {
    await ElMessageBox.confirm(`确定强制结束会话 "${row.fileName}" 吗？`, '强制结束', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await adminApi.forceEndSession(row.id)
    ElMessage.success('会话已强制结束')
    row.status = 'ENDED'; row.endedAt = new Date().toISOString()
  } catch (err) { ElMessage.error(err.response?.data?.message || '操作失败') }
}

async function batchForceEnd() {
  const names = selectedRows.value.filter(r => r.status === 'CREATED' || r.status === 'ACTIVE')
  if (names.length === 0) { ElMessage.warning('所选会话均已结束'); return }
  try {
    await ElMessageBox.confirm(
      `确定批量强制结束 ${names.length} 个会话吗？所有参与者将被断开。`,
      '批量强制结束', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
  } catch { return }
  let ok = 0, fail = 0
  await Promise.all(names.map(r =>
    adminApi.forceEndSession(r.id).then(() => { ok++; r.status = 'ENDED' }).catch(() => fail++)
  ))
  ElMessage.success(`已结束 ${ok} 个${fail > 0 ? `，${fail} 个失败` : ''}`)
  loadData()
}

loadData()
</script>

<style scoped>
.admin-table-page { max-width: 100%; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.text-muted { color: #c0c4cc; font-size: 13px; }
</style>
