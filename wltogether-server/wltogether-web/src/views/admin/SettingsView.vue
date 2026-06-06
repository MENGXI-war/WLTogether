<template>
  <div class="settings-page">
    <el-card header="系统配置" shadow="hover">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="configKey" label="配置键" min-width="240">
          <template #default="{ row }">
            <code class="config-key">{{ row.configKey }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="当前值" min-width="200" show-overflow-tooltip />
        <el-table-column prop="valueType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.valueType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button
              v-if="isWritable(row.configKey)"
              type="primary"
              size="small"
              link
              @click="openEditDialog(row)"
            >
              修改
            </el-button>
            <span v-else class="text-muted">只读</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="修改配置" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="editKey">
          <el-input v-model="form.value" :placeholder="'输入新值（当前：' + currentValue + '）'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import adminApi from '@/api/admin'

const WRITABLE_KEYS = [
  'transfer.server_relay.enabled',
  'transfer.server_relay.max_file_mb',
  'session.max_participants',
  'session.max_concurrent_per_user'
]

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editKey = ref('')
const currentValue = ref('')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ value: '' })
const rules = {
  value: [{ required: true, message: '值不能为空', trigger: 'blur' }]
}

function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}
function isWritable(key) {
  return WRITABLE_KEYS.includes(key)
}

async function loadData() {
  loading.value = true
  try {
    tableData.value = await adminApi.getSettings()
  } finally {
    loading.value = false
  }
}

function openEditDialog(row) {
  editKey.value = row.configKey
  currentValue.value = row.configValue
  form.value = row.configValue || ''
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await adminApi.updateSetting(editKey.value, form.value)
    ElMessage.success('配置已更新')
    dialogVisible.value = false
    loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '更新失败')
  } finally {
    saving.value = false
  }
}

loadData()
</script>

<style scoped>
.settings-page {
  max-width: 100%;
}
.config-key {
  font-size: 12px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
}
.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>
