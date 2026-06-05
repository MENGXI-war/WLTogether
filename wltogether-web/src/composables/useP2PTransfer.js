import { ref } from 'vue'
import WebTorrent from 'webtorrent'
import filesApi from '@/api/files'

// Private tracker announce URL
const TRACKER_URL = '/api/tracker/announce'

export function useP2PTransfer() {
  const client = ref(null)
  const downloading = ref(false)
  const seeding = ref(false)
  const downloadProgress = ref(0)
  const uploadProgress = ref(0)
  const downloadSpeed = ref('')
  const uploadSpeed = ref('')
  const peers = ref(0)
  const fileUrl = ref(null)
  const transferMode = ref('p2p') // 'p2p' | 'relay'

  function getClient() {
    if (!client.value) {
      client.value = new WebTorrent({
        tracker: {
          announce: [TRACKER_URL]
        }
      })
    }
    return client.value
  }

  // =============== Server Relay Methods ===============

  /**
   * Upload file via server relay (HTTP).
   * Returns { fileId, fileName, fileSize } on success.
   */
  async function uploadRelay(file, onProgress) {
    transferMode.value = 'relay'
    try {
      const res = await filesApi.upload(file, (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const pct = Math.round((progressEvent.loaded / progressEvent.total) * 100)
          onProgress(pct)
        }
      })
      // Backend returns ApiResponse with data wrapper
      return res.data || res
    } finally {
      transferMode.value = 'p2p'
    }
  }

  /**
   * Download file via server relay (HTTP).
   * Returns a blob URL for the downloaded file.
   */
  async function downloadRelay(fileId) {
    transferMode.value = 'relay'
    try {
      const blob = await filesApi.download(fileId)
      const url = URL.createObjectURL(blob)
      fileUrl.value = url
      return url
    } finally {
      transferMode.value = 'p2p'
    }
  }

  /**
   * Seed a local file to create a torrent.
   * Returns magnet URI and .torrent buffer.
   */
  async function seedFile(file) {
    seeding.value = true
    const wt = getClient()

    return new Promise((resolve, reject) => {
      wt.seed(file, { announce: [TRACKER_URL] }, (torrent) => {
        seeding.value = false
        resolve({
          magnetUri: torrent.magnetURI,
          infoHash: torrent.infoHash
        })
      })

      wt.on('error', (err) => {
        seeding.value = false
        reject(err)
      })
    })
  }

  /**
   * Download a file from a magnet URI.
   * Returns a blob URL for the downloaded file.
   */
  async function downloadFile(magnetUri) {
    downloading.value = true
    downloadProgress.value = 0
    const wt = getClient()

    return new Promise((resolve, reject) => {
      wt.add(magnetUri, (torrent) => {
        // Find the largest file (the media file)
        const file = torrent.files.reduce((a, b) => a.length > b.length ? a : b)

        torrent.on('download', () => {
          downloadProgress.value = Math.round(torrent.progress * 100)
          downloadSpeed.value = formatSpeed(torrent.downloadSpeed)
          uploadSpeed.value = formatSpeed(torrent.uploadSpeed)
          peers.value = torrent.numPeers
        })

        torrent.on('upload', () => {
          uploadProgress.value = Math.round(torrent.ratio * 100)
        })

        torrent.on('done', () => {
          downloading.value = false
          file.getBlobURL((err, url) => {
            if (err) {
              reject(err)
            } else {
              fileUrl.value = url
              resolve(url)
            }
          })
        })

        torrent.on('error', (err) => {
          downloading.value = false
          reject(err)
        })
      })

      wt.on('error', (err) => {
        downloading.value = false
        reject(err)
      })
    })
  }

  function formatSpeed(bytesPerSec) {
    if (!bytesPerSec || bytesPerSec === 0) return '0 B/s'
    const units = ['B/s', 'KB/s', 'MB/s', 'GB/s']
    let i = 0
    let speed = bytesPerSec
    while (speed >= 1024 && i < units.length - 1) {
      speed /= 1024
      i++
    }
    return `${speed.toFixed(1)} ${units[i]}`
  }

  function destroy() {
    if (client.value) {
      client.value.destroy()
      client.value = null
    }
  }

  return {
    downloading,
    seeding,
    downloadProgress,
    uploadProgress,
    downloadSpeed,
    uploadSpeed,
    peers,
    fileUrl,
    transferMode,
    seedFile,
    downloadFile,
    uploadRelay,
    downloadRelay,
    destroy
  }
}
