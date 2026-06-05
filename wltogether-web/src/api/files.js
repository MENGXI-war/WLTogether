import api from './index'

export default {
  upload(file, onProgress) {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/api/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0, // no timeout for large files
      onUploadProgress: onProgress
    })
  },
  download(fileId) {
    return api.get(`/api/files/${fileId}`, {
      responseType: 'blob',
      timeout: 0
    })
  },
  getInfo(fileId) {
    return api.get(`/api/files/${fileId}/info`)
  },
  remove(fileId) {
    return api.delete(`/api/files/${fileId}`)
  }
}
