<template>
  <div class="admin-table-page">
    <div class="toolbar">
      <el-button type="primary" @click="openCreateDialog">新建壁纸</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="60" align="center" />
      <el-table-column label="预览" width="140" align="center">
        <template #default="{ row }">
          <img v-if="row.imageUrl" :src="row.imageUrl" class="wallpaper-thumb" @error="onImgError" />
          <span v-else class="text-muted">无图片</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140">
        <template #default="{ row }">
          {{ row.title || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.description || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="imageUrl" label="图片URL" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <code class="url-text">{{ row.imageUrl }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="publishDate" label="发布日期" width="120" align="center">
        <template #default="{ row }">
          {{ row.publishDate || '-' }}
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑壁纸' : '新建壁纸'" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="图片URL" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="输入图片URL" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="输入壁纸标题（选填）" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="输入壁纸描述（选填）" />
        </el-form-item>
        <el-form-item label="发布日期">
          <el-date-picker
            v-model="form.publishDate"
            type="date"
            placeholder="选填，默认今天"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="form.imageUrl" label="预览">
          <img :src="form.imageUrl" class="wallpaper-preview" @error="onPreviewError" />
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
import adminApi from '@/api/admin'

const tableData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  imageUrl: '',
  title: '',
  description: '',
  publishDate: null
})
const rules = {
  imageUrl: [{ required: true, message: '图片URL不能为空', trigger: 'blur' }]
}

function formatTime(instant) {
  if (!instant) return '-'
  return new Date(instant).toLocaleString('zh-CN')
}
function onImgError(e) {
  e.target.style.display = 'none'
}
function onPreviewError(e) {
  e.target.style.display = 'none'
}

async function loadData() {
  loading.value = true
  try {
    const res = await adminApi.getWallpapers()
    tableData.value = res
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.imageUrl = ''
  form.title = ''
  form.description = ''
  form.publishDate = null
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.imageUrl = row.imageUrl
  form.title = row.title || ''
  form.description = row.description || ''
  form.publishDate = row.publishDate || null
  dialogVisible.value = true
}

async function onSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = {
      imageUrl: form.imageUrl,
      title: form.title,
      description: form.description,
      publishDate: form.publishDate
    }
    if (isEdit.value) {
      await adminApi.updateWallpaper(editId.value, data)
      ElMessage.success('壁纸已更新')
    } else {
      await adminApi.createWallpaper(data)
      ElMessage.success('壁纸已创建')
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
    await ElMessageBox.confirm(`确定删除壁纸 "${row.title || row.id}" 吗？`, '删除壁纸', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch { return }

  try {
    await adminApi.deleteWallpaper(row.id)
    ElMessage.success('壁纸已删除')
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
}
.wallpaper-thumb {
  width: 100px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
}
.wallpaper-preview {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  object-fit: contain;
}
.url-text {
  font-size: 11px;
  color: #909399;
  word-break: break-all;
}
.text-muted {
  color: #c0c4cc;
}
</style>
