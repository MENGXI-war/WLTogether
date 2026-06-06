<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable @change="loadData(1)" style="width: 140px">
        <el-option label="全部" value="" />
        <el-option label="待验证" value="PENDING" />
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已禁用" value="DISABLED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
      <el-button v-if="selectedRows.length > 0" type="danger" @click="batchDelete">
        批量删除 ({{ selectedRows.length }})
      </el-button>
    </div>

    <el-table ref="tableRef" :data="tableData" v-loading="loading" stripe border style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="40" />
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="180" />
      <el-table-column prop="nickname" label="昵称" min-width="120">
        <template #default="{ row }">
          {{ row.nickname || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="role" label="角色" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">
            {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="uid" label="UID" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.uid" class="uid-text">{{ row.uid }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="emailVerified" label="已验证" width="70" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.emailVerified" color="#67c23a"><CircleCheck /></el-icon>
          <el-icon v-else color="#f56c6c"><CircleClose /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.role !== 'ADMIN'"
            :type="row.status === 'DISABLED' ? 'success' : 'warning'"
            size="small"
            link
            @click="toggleStatus(row)"
          >
            {{ row.status === 'DISABLED' ? '启用' : '禁用' }}
          </el-button>
          <el-button
            v-if="row.role !== 'ADMIN'"
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="() => loadData(1)"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const statusFilter = ref('')
const selectedRows = ref([])

function onSelectionChange(rows) { selectedRows.value = rows }

function statusTagType(status) {
  const map = { ACTIVE: 'success', PENDING: 'warning', DISABLED: 'danger', DELETED: 'info' }
  return map[status] || 'info'
}
function statusLabel(status) {
  const map = { ACTIVE: '正常', PENDING: '待验证', DISABLED: '已禁用', DELETED: '已删除' }
  return map[status] || status
}

function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}

async function loadData(pg) {
  if (pg) page.value = pg
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value, sort: 'createdAt,desc' }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await adminApi.getUsers(params)
    tableData.value = res.content
    total.value = res.totalElements
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'DISABLED' ? 'ACTIVE' : 'DISABLED'
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${action}用户 "${row.username}" 吗？`, action + '用户', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
  } catch { return }

  try {
    await adminApi.updateUserStatus(row.id, newStatus)
    ElMessage.success(`用户已${action}`)
    row.status = newStatus
  } catch (err) {
    ElMessage.error(err.response?.data?.message || `${action}失败`)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户 "${row.username}" 吗？此操作不可恢复。`, '删除用户', {
      type: 'error',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch { return }

  try {
    await adminApi.deleteUser(row.id)
    ElMessage.success('用户已删除')
    loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

async function batchDelete() {
  const names = selectedRows.value.map(r => r.username).join('、')
  try {
    await ElMessageBox.confirm(
      `确定批量删除 ${selectedRows.value.length} 个用户（${names}）吗？此操作不可恢复。`,
      '批量删除用户',
      { type: 'error', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch { return }

  let ok = 0, fail = 0
  const promises = selectedRows.value
    .filter(r => r.role !== 'ADMIN')
    .map(r => adminApi.deleteUser(r.id).then(() => ok++).catch(() => fail++))
  await Promise.all(promises)
  if (ok > 0) ElMessage.success(`已删除 ${ok} 个用户` + (fail > 0 ? `，${fail} 个失败` : ''))
  else ElMessage.error('全部删除失败')
  loadData()
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
.uid-text {
  font-family: monospace;
  font-size: 12px;
  color: #909399;
}
.text-muted {
  color: #c0c4cc;
}
</style>
