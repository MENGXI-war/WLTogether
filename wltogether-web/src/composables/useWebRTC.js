import { ref } from 'vue'
import { useWebSocket } from './useWebSocket'

// Google public STUN server
const ICE_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' }
  ]
}

export function useWebRTC() {
  const { send, subscribe } = useWebSocket()
  // Map of userId -> RTCPeerConnection
  const connections = ref(new Map())
  // Map of userId -> RTCDataChannel
  const channels = ref(new Map())
  const connected = ref(new Set())
  const connecting = ref(false)

  // Callbacks set by consumer
  let onDataCallback = null
  let onConnectionCallback = null

  /**
   * Start listening for incoming WebRTC signals from other peers.
   */
  function listen() {
    return subscribe('/user/queue/signal', (message) => {
      try {
        const signal = JSON.parse(message.body)
        if (signal.type === 'p2p.signal') {
          handleSignal(signal)
        }
      } catch (e) {
        console.error('[WebRTC] Failed to parse signal:', e)
      }
    })
  }

  /**
   * Handle an incoming signaling message.
   */
  async function handleSignal(signal) {
    const { fromUserId, sdp, candidate } = signal
    if (!fromUserId) return

    try {
      if (sdp) {
        // It's an offer or answer
        if (sdp.type === 'offer') {
          await handleOffer(fromUserId, signal)
        } else if (sdp.type === 'answer') {
          await handleAnswer(fromUserId, sdp)
        }
      } else if (candidate) {
        await handleCandidate(fromUserId, candidate)
      }
    } catch (e) {
      console.error('[WebRTC] Signal handling error:', e)
    }
  }

  /**
   * Create and send an offer to a target user.
   */
  async function createOffer(targetUserId, sessionId) {
    const pc = createPeerConnection(targetUserId)
    const dataChannel = pc.createDataChannel('filetransfer', {
      ordered: true
    })
    setupDataChannel(dataChannel, targetUserId)

    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)

    sendSignal(targetUserId, sessionId, { sdp: pc.localDescription })
  }

  /**
   * Handle an incoming offer.
   */
  async function handleOffer(fromUserId, signal) {
    const pc = createPeerConnection(fromUserId)

    // Handle incoming data channel
    pc.ondatachannel = (event) => {
      setupDataChannel(event.channel, fromUserId)
    }

    await pc.setRemoteDescription(new RTCSessionDescription(signal.sdp))
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)

    sendSignal(fromUserId, signal.sessionId, { sdp: pc.localDescription })
  }

  /**
   * Handle an incoming answer.
   */
  async function handleAnswer(fromUserId, sdp) {
    const pc = connections.value.get(fromUserId)
    if (!pc) return
    await pc.setRemoteDescription(new RTCSessionDescription(sdp))
  }

  /**
   * Handle an incoming ICE candidate.
   */
  async function handleCandidate(fromUserId, candidate) {
    const pc = connections.value.get(fromUserId)
    if (!pc) return
    try {
      await pc.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (e) {
      console.error('[WebRTC] ICE candidate error:', e)
    }
  }

  /**
   * Create a new RTCPeerConnection for a peer.
   */
  function createPeerConnection(targetUserId) {
    // Close existing connection if any
    const existing = connections.value.get(targetUserId)
    if (existing) {
      existing.close()
    }

    const pc = new RTCPeerConnection(ICE_SERVERS)
    connections.value.set(targetUserId, pc)

    pc.onicecandidate = (event) => {
      if (event.candidate) {
        sendSignal(targetUserId, null, { candidate: event.candidate })
      }
    }

    pc.onconnectionstatechange = () => {
      if (pc.connectionState === 'connected') {
        connected.value.add(targetUserId)
        onConnectionCallback?.(targetUserId, 'connected')
      } else if (pc.connectionState === 'disconnected' || pc.connectionState === 'failed') {
        connected.value.delete(targetUserId)
        channels.value.delete(targetUserId)
        onConnectionCallback?.(targetUserId, 'disconnected')
      }
    }

    return pc
  }

  /**
   * Set up a data channel for file transfer.
   */
  function setupDataChannel(channel, peerId) {
    channels.value.set(peerId, channel)

    channel.onopen = () => {
      connected.value.add(peerId)
      onConnectionCallback?.(peerId, 'connected')
    }

    channel.onclose = () => {
      channels.value.delete(peerId)
      connected.value.delete(peerId)
    }

    channel.onmessage = (event) => {
      if (onDataCallback) {
        onDataCallback(peerId, event.data)
      }
    }
  }

  /**
   * Send data to a specific peer via DataChannel.
   */
  function sendData(targetUserId, data) {
    const channel = channels.value.get(targetUserId)
    if (!channel || channel.readyState !== 'open') {
      console.warn('[WebRTC] DataChannel not open for user', targetUserId)
      return false
    }

    // Handle chunked sending for large data
    if (data instanceof ArrayBuffer || data instanceof Uint8Array) {
      const CHUNK_SIZE = 16 * 1024 // 16KB chunks
      const bytes = data instanceof Uint8Array ? data : new Uint8Array(data)
      let offset = 0
      while (offset < bytes.byteLength) {
        const chunk = bytes.slice(offset, offset + CHUNK_SIZE)
        channel.send(chunk)
        offset += CHUNK_SIZE
      }
    } else {
      channel.send(data)
    }
    return true
  }

  /**
   * Send a signaling message via STOMP.
   */
  function sendSignal(targetUserId, sessionId, payload) {
    send('/p2p.signal', {
      targetUserId,
      sessionId,
      ...payload
    })
  }

  /**
   * Register callbacks.
   */
  function onData(callback) {
    onDataCallback = callback
  }

  function onConnection(callback) {
    onConnectionCallback = callback
  }

  /**
   * Close all connections.
   */
  function closeAll() {
    connections.value.forEach((pc, key) => {
      pc.close()
    })
    connections.value.clear()
    channels.value.clear()
    connected.value.clear()
  }

  return {
    connected,
    connecting,
    listen,
    createOffer,
    sendData,
    onData,
    onConnection,
    closeAll
  }
}
