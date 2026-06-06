<template>
  <div class="admin-table-page">
    <div class="toolbar" v-if="selectedRows.length > 0">
      <el-button type="danger" @click="batchDelete">批量删除记录 ({{ selectedRows.length }})</el-button>
    </div>

    <el-table ref="tableRef" :data="tableData" v-loading="loading" stripe border style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="fileHash" label="文件哈希" width="140" show-overflow-tooltip>
        <template #default="{ row }"><code class="hash-text">{{ row.fileHash }}</code></template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="fileSize" label="大小" width="100" align="right">
        <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="mediaType" label="媒体类型" width="90" align="center">
        <template #default="{ row }">{{ row.mediaType || '-' }}</template>
      </el-table-column>
      <el-table-column prop="durationSeconds" label="时长" width="80" align="center">
        <template #default="{ row }">{{ formatDuration(row.durationSeconds) }}</template>
      </el-table-column>
      <el-table-column prop="videoCodec" label="视频编码" width="100" align="center">
        <template #default="{ row }">{{ row.videoCodec || '-' }}</template>
      </el-table-column>
      <el-table-column prop="audioCodec" label="音频编码" width="100" align="center">
        <template #default="{ row }">{{ row.audioCodec || '-' }}</template>
      </el-table-column>
      <el-table-column prop="firstSharedBy" label="首发用户ID" width="100" align="center">
        <template #default="{ row }">{{ row.firstSharedBy || '-' }}</template>
      </el-table-column>
      <el-table-column prop="shareCount" label="共享次数" width="90" align="center" />
      <el-table-column prop="createdAt" label="首次记录" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除记录</el-button>
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

const tableData = ref([]); const loading = ref(false); const page = ref(1); const size = ref(20); const total = ref(0)
const selectedRows = ref([])
function onSelectionChange(rows) { selectedRows.value = rows }

function formatTime(i) { return i ? new Date(i).toLocaleString('zh-CN') : '-' }
function formatBytes(b) { if (!b) return '0 B'; const u = ['B','KB','MB','GB','TB']; let i = 0, v = b; while (v >= 1024 && i < u.length - 1) { v /= 1024; i++ } return v.toFixed(1) + ' ' + u[i] }
function formatDuration(s) { if (!s) return '-'; const m = Math.floor(s / 60), sec = s % 60; return m >= 60 ? `${Math.floor(m/60)}:${String(m%60).padStart(2,'0')}:${String(sec).padStart(2,'0')}` : `${m}:${String(sec).padStart(2,'0')}` }

async function loadData(pg) {
  if (pg) page.value = pg; loading.value = true
  try { const res = await adminApi.getFiles({ page: page.value - 1, size: size.value, sort: 'createdAt,desc' }); tableData.value = res.content; total.value = res.totalElements }
  finally { loading.value = false }
}

async function handleDelete(row) {
  try { await ElMessageBox.confirm(`确定删除文件记录 "${row.fileName}" 吗？`, '删除', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }) } catch { return }
  try { await adminApi.deleteFile(row.id); ElMessage.success('已删除'); loadData() } catch (err) { ElMessage.error(err.response?.data?.message || '删除失败') }
}

async function batchDelete() {
  try { await ElMessageBox.confirm(`确定批量删除 ${selectedRows.value.length} 条文件记录吗？`, '批量删除', { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }) } catch { return }
  let ok = 0, fail = 0
  await Promise.all(selectedRows.value.map(r => adminApi.deleteFile(r.id).then(() => ok++).catch(() => fail++)))
  ElMessage.success(`已删除 ${ok} 条${fail > 0 ? `，${fail} 条失败` : ''}`); loadData()
}

loadData()
</script>

<style scoped>
.admin-table-page { max-width: 100%; }
.toolbar { margin-bottom: 16px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.hash-text { font-size: 11px; background: #f5f7fa; padding: 2px 4px; border-radius: 3px; word-break: break-all; }
</style>
