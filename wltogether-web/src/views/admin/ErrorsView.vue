<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="loadData(1)" style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="未处理" value="OPEN" />
        <el-option label="处理中" value="IN_PROGRESS" />
        <el-option label="已解决" value="RESOLVED" />
      </el-select>
      <el-select v-model="severityFilter" placeholder="按严重程度" clearable @change="loadData(1)" style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="INFO" value="INFO" />
        <el-option label="WARN" value="WARN" />
        <el-option label="ERROR" value="ERROR" />
        <el-option label="FATAL" value="FATAL" />
      </el-select>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="occurredAt" label="发生时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.occurredAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="errorType" label="错误类型" width="130" show-overflow-tooltip />
      <el-table-column prop="message" label="错误信息" min-width="200" show-overflow-tooltip />
      <el-table-column prop="source" label="来源" width="80" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.source === 'CLIENT' ? 'warning' : ''">
            {{ row.source }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="severity" label="严重程度" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="severityTag(row.severity)" size="small">
            {{ row.severity }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="errorStatusTag(row.status)" size="small">
            {{ errorStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="handledByDegradation" label="已降级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.handledByDegradation ? 'success' : 'info'" size="small">
            {{ row.handledByDegradation ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resolutionNote" label="处理备注" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.resolutionNote || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center" fixed="right">
        <template #default="{ row }">
          <el-dropdown @command="(cmd) => handleStatusChange(row, cmd)" v-if="row.status !== 'RESOLVED'">
            <el-button type="primary" size="small" link>
              更新状态 <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="OPEN">设为未处理</el-dropdown-item>
                <el-dropdown-item command="IN_PROGRESS">设为处理中</el-dropdown-item>
                <el-dropdown-item command="RESOLVED">设为已解决</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span v-else class="text-muted">已解决</span>
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
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const statusFilter = ref('')
const severityFilter = ref('')

function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}
function severityTag(severity) {
  const map = { INFO: 'info', WARN: 'warning', ERROR: 'danger', FATAL: 'danger' }
  return map[severity] || 'info'
}
function errorStatusTag(status) {
  const map = { OPEN: 'danger', IN_PROGRESS: 'warning', RESOLVED: 'success' }
  return map[status] || 'info'
}
function errorStatusLabel(status) {
  const map = { OPEN: '未处理', IN_PROGRESS: '处理中', RESOLVED: '已解决' }
  return map[status] || status
}

async function loadData(pg) {
  if (pg) page.value = pg
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value, sort: 'occurredAt,desc' }
    if (statusFilter.value) params.status = statusFilter.value
    if (severityFilter.value) params.severity = severityFilter.value
    const res = await adminApi.getErrors(params)
    tableData.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(row, newStatus) {
  try {
    await adminApi.updateErrorStatus(row.id, { status: newStatus, resolutionNote: row.resolutionNote })
    row.status = newStatus
    ElMessage.success(`状态已更新为 "${errorStatusLabel(newStatus)}"`)
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '更新失败')
  }
}

loadData()
</script>

<style scoped>
.admin-table-page {
  max-width: 100%;
}
.toolbar {
  margin-bottom: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
