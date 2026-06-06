<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-input v-model="filterGroupId" placeholder="按群组ID筛选" clearable @change="loadData(1)" style="width: 160px" />
      <el-input v-model="filterSenderId" placeholder="按发送者ID筛选" clearable @change="loadData(1)" style="width: 160px" />
      <el-button v-if="selectedRows.length > 0" type="danger" @click="batchDelete">批量删除 ({{ selectedRows.length }})</el-button>
    </div>

    <el-table ref="tableRef" :data="tableData" v-loading="loading" stripe border style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="groupId" label="群组ID" width="80" align="center" />
      <el-table-column prop="senderId" label="发送者ID" width="90" align="center" />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.messageType === 'IMAGE'" class="msg-type-tag">[图片]</span>
          <span v-if="row.messageType === 'FILE'" class="msg-type-tag">[文件]</span>
          {{ row.content || '' }}
        </template>
      </el-table-column>
      <el-table-column prop="messageType" label="类型" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.messageType === 'IMAGE' ? 'warning' : row.messageType === 'FILE' ? 'danger' : ''">{{ row.messageType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="replyToId" label="回复ID" width="80" align="center">
        <template #default="{ row }">{{ row.replyToId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sessionId" label="会话ID" width="80" align="center">
        <template #default="{ row }">{{ row.sessionId || '-' }}</template>
      </el-table-column>
      <el-table-column prop="isPinned" label="置顶" width="70" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isPinned" color="#e6a23c"><Star /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发送时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
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
import { Star } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterGroupId = ref('')
const filterSenderId = ref('')
const selectedRows = ref([])
function onSelectionChange(rows) { selectedRows.value = rows }

function formatTime(i) { return i ? new Date(i).toLocaleString('zh-CN') : '-' }

async function loadData(pg) {
  if (pg) page.value = pg
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value, sort: 'createdAt,desc' }
    if (filterGroupId.value) params.groupId = filterGroupId.value
    if (filterSenderId.value) params.senderId = filterSenderId.value
    const res = await adminApi.getMessages(params)
    tableData.value = res.content; total.value = res.totalElements
  } finally { loading.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该消息吗？', '删除消息', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
  } catch { return }
  try { await adminApi.deleteMessage(row.id); ElMessage.success('消息已删除'); loadData() }
  catch (err) { ElMessage.error(err.response?.data?.message || '删除失败') }
}

async function batchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定批量删除 ${selectedRows.value.length} 条消息吗？此操作不可恢复。`,
      '批量删除消息', { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch { return }
  let ok = 0, fail = 0
  await Promise.all(selectedRows.value.map(r =>
    adminApi.deleteMessage(r.id).then(() => ok++).catch(() => fail++)
  ))
  ElMessage.success(`已删除 ${ok} 条${fail > 0 ? `，${fail} 条失败` : ''}`)
  loadData()
}

loadData()
</script>

<style scoped>
.admin-table-page { max-width: 100%; }
.toolbar { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.msg-type-tag { color: #909399; font-size: 12px; margin-right: 4px; }
</style>
