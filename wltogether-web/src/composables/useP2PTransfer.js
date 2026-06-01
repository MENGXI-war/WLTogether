import { ref } from 'vue'
import WebTorrent from 'webtorrent'

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

  function getClient() {
    if (!client.value) {
      client.value = new WebTorrent()
    }
    return client.value
  }

  /**
   * Seed a local file to create a torrent.
   * Returns magnet URI and .torrent buffer.
   */
  async function seedFile(file) {
    seeding.value = true
    const wt = getClient()

    return new Promise((resolve, reject) => {
      wt.seed(file, (torrent) => {
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
    seedFile,
    downloadFile,
    destroy
  }
}
