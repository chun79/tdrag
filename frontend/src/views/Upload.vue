<template>
  <div class="upload-container">
    <div class="upload-header">
      <h2>文档上传</h2>
      <p>上传文档以构建知识库，支持PDF、Word、TXT等格式</p>
    </div>
    
    <div class="upload-section">
      <el-upload
        class="upload-dragger"
        drag
        action="/api/documents/upload"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        multiple
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、DOC、DOCX、TXT 格式，单个文件不超过 50MB
          </div>
        </template>
      </el-upload>
    </div>
    
    <div class="documents-section">
      <h3>已上传文档</h3>
      <el-table :data="documents" v-loading="loading" style="width: 100%">
        <el-table-column prop="originalFilename" label="文档名称" />
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="scope">
            {{ formatFileSize(scope.row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag 
              :type="getStatusType(scope.row.status)"
            >
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.uploadTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteDocument(scope.row)"
              :loading="scope.row.deleting"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'

const documents = ref([])
const loading = ref(false)

const handleUploadSuccess = (_response: any, file: any) => {
  ElMessage.success(`${file.name} 上传成功`)
  loadDocuments()
}

const handleUploadError = (error: any, file: any) => {
  console.error('Upload error:', error)
  ElMessage.error(`${file.name} 上传失败`)
}

const beforeUpload = (file: any) => {
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain'
  ]
  
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只支持 PDF、DOC、DOCX、TXT 格式的文件')
    return false
  }
  
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  
  return true
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/documents/list')
    documents.value = response.data.content || []
  } catch (error) {
    console.error('Failed to fetch documents:', error)
  } finally {
    loading.value = false
  }
}

const deleteDocument = async (document: any) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档 "${document.originalFilename}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 设置删除状态
    document.deleting = true
    
    await axios.delete(`/api/documents/${document.documentId}`)
    ElMessage.success('文档删除成功')
    loadDocuments()
    
  } catch (error: any) {
    if (error === 'cancel') {
      // 用户取消删除
      return
    }
    console.error('Failed to delete document:', error)
    ElMessage.error('文档删除失败')
  } finally {
    // 清除删除状态
    document.deleting = false
  }
}

const formatFileSize = (fileSize: number) => {
  if (fileSize < 1024) {
    return fileSize + ' B'
  } else if (fileSize < 1024 * 1024) {
    return (fileSize / 1024).toFixed(2) + ' KB'
  } else if (fileSize < 1024 * 1024 * 1024) {
    return (fileSize / (1024 * 1024)).toFixed(2) + ' MB'
  } else {
    return (fileSize / (1024 * 1024 * 1024)).toFixed(2) + ' GB'
  }
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'PROCESSING':
      return 'warning'
    case 'FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'PROCESSING':
      return '处理中'
    case 'FAILED':
      return '处理失败'
    default:
      return status || '未知状态'
  }
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleString()
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.upload-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.upload-header {
  text-align: center;
  margin-bottom: 30px;
}

.upload-header h2 {
  color: var(--el-color-primary);
  margin-bottom: 10px;
}

.upload-header p {
  color: var(--el-text-color-regular);
}

.upload-section {
  margin-bottom: 40px;
}

.documents-section {
  background: var(--el-bg-color);
  border-radius: 8px;
  padding: 20px;
}

.documents-section h3 {
  margin: 0 0 20px 0;
  color: var(--el-color-primary);
}
</style> 