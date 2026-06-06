<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-checkbox v-model="publishedOnly" @change="loadData">仅显示已发布</el-checkbox>
      <el-button type="primary" @click="openCreateDialog">新建公告</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          {{ truncate(row.content, 100) }}
        </template>
      </el-table-column>
      <el-table-column prop="isPinned" label="置顶" width="70" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isPinned" color="#e6a23c"><Star /></el-icon>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="isPublished" label="已发布" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isPublished ? 'success' : 'info'" size="small">
            {{ row.isPublished ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.publishedAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="expiredAt" label="过期时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.expiredAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
          <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新建公告'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="输入公告内容" />
        </el-form-item>
        <el-form-item label="是否置顶">
          <el-switch v-model="form.isPinned" />
        </el-form-item>
        <el-form-item label="是否发布">
          <el-switch v-model="form.isPublished" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="form.expiredAt"
            type="datetime"
            placeholder="选填，留空表示永不过期"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss.SSS[Z]"
            clearable
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)
const publishedOnly = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  title: '',
  content: '',
  isPinned: false,
  isPublished: false,
  expiredAt: null
})
const rules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  content: [{ required: true, message: '内容不能为空', trigger: 'blur' }]
}

function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}
function truncate(str, max) {
  if (!str) return ''
  return str.length > max ? str.substring(0, max) + '…' : str
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getAnnouncements({ publishedOnly: publishedOnly.value })
    tableData.value = res
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.title = ''
  form.content = ''
  form.isPinned = false
  form.isPublished = false
  form.expiredAt = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.title = row.title
  form.content = row.content
  form.isPinned = row.isPinned
  form.isPublished = row.isPublished
  form.expiredAt = row.expiredAt ? new Date(row.expiredAt).toISOString() : null
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = {
      title: form.title,
      content: form.content,
      isPinned: form.isPinned,
      isPublished: form.isPublished,
      expiredAt: form.expiredAt || null
    }
    if (isEdit.value) {
      await adminApi.updateAnnouncement(editId.value, data)
      ElMessage.success('公告已更新')
    } else {
      await adminApi.createAnnouncement(data)
      ElMessage.success('公告已创建')
    }
    dialogVisible.value = false
    loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除公告 "${row.title}" 吗？`, '删除公告', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch { return }

  try {
    await adminApi.deleteAnnouncement(row.id)
    ElMessage.success('公告已删除')
    loadData()
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '删除失败')
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
  gap: 16px;
  align-items: center;
}
</style>
