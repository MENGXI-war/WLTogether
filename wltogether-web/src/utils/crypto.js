/**
 * WLTogether offline package crypto utilities.
 * Uses Web Crypto API (SubtleCrypto) for AES-256-GCM and ECDSA P-256.
 */

// .wlp format magic bytes
const WLP_MAGIC = new Uint8Array([0x57, 0x4C, 0x50, 0x01]) // "WLP\1"

// .wlk key file magic bytes
const WLK_MAGIC = new Uint8Array([0x57, 0x4C, 0x4B, 0x01]) // "WLK\1"

/**
 * Generate a random AES-256-GCM key.
 */
export async function generateAesKey() {
  return await crypto.subtle.generateKey(
    { name: 'AES-GCM', length: 256 },
    true,
    ['encrypt', 'decrypt']
  )
}

/**
 * Export an AES key to raw bytes.
 */
export async function exportAesKey(key) {
  const raw = await crypto.subtle.exportKey('raw', key)
  return new Uint8Array(raw)
}

/**
 * Import an AES key from raw bytes.
 */
export async function importAesKey(raw) {
  return await crypto.subtle.importKey(
    'raw',
    raw,
    { name: 'AES-GCM', length: 256 },
    true,
    ['encrypt', 'decrypt']
  )
}

/**
 * AES-256-GCM encrypt data.
 * Returns { ciphertext, iv } as Uint8Arrays.
 */
export async function aesEncrypt(key, plaintext) {
  const iv = crypto.getRandomValues(new Uint8Array(12)) // 96-bit IV for GCM
  const ciphertext = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    plaintext
  )
  return {
    ciphertext: new Uint8Array(ciphertext),
    iv
  }
}

/**
 * AES-256-GCM decrypt data.
 */
export async function aesDecrypt(key, ciphertext, iv) {
  return await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    key,
    ciphertext
  )
}

/**
 * Generate an ECDSA P-256 key pair for signing.
 */
export async function generateSigningKeyPair() {
  return await crypto.subtle.generateKey(
    { name: 'ECDSA', namedCurve: 'P-256' },
    true,
    ['sign', 'verify']
  )
}

/**
 * Sign data with ECDSA P-256.
 */
export async function signData(privateKey, data) {
  const signature = await crypto.subtle.sign(
    { name: 'ECDSA', hash: { name: 'SHA-256' } },
    privateKey,
    data
  )
  return new Uint8Array(signature)
}

/**
 * Verify ECDSA P-256 signature.
 */
export async function verifySignature(publicKey, data, signature) {
  return await crypto.subtle.verify(
    { name: 'ECDSA', hash: { name: 'SHA-256' } },
    publicKey,
    data,
    signature
  )
}

/**
 * Build a .wlp offline package.
 *
 * Format:
 * [4 bytes magic "WLP\1"]
 * [1 byte flags: bit0=groupBound, bit1=aesEncrypted, bit2=signed]
 * [8 bytes groupId (big-endian, 0 if not groupBound)]
 * [if signed: 2 bytes sigLen | sigLen bytes signature]
 * [if encrypted: 12 bytes IV | ciphertext]
 * [if not encrypted: plaintext]
 *
 * @param {File} file - The original file to package
 * @param {object} options - { groupBound, groupId, aesKey, signingKey, ttl }
 * @returns {Uint8Array} - The .wlp package bytes
 */
export async function buildWlp(file, options = {}) {
  const { groupBound, groupId, aesKey, signingKey } = options

  // Read file as ArrayBuffer
  const fileData = await file.arrayBuffer()

  let flags = 0
  if (groupBound) flags |= 1
  if (aesKey) flags |= 2
  if (signingKey) flags |= 4

  const headerParts = [WLP_MAGIC, new Uint8Array([flags])]

  // Group ID
  const groupIdBytes = new Uint8Array(8)
  if (groupBound && groupId) {
    const view = new DataView(groupIdBytes.buffer)
    view.setBigUint64(0, BigInt(groupId), false)
  }
  headerParts.push(groupIdBytes)

  // File metadata (original name + size)
  const encoder = new TextEncoder()
  const nameBytes = encoder.encode(file.name)
  const metaLen = 4 + nameBytes.length + 8 // nameLen(4) + name + fileSize(8)
  const metaBytes = new Uint8Array(metaLen)
  const metaView = new DataView(metaBytes.buffer)
  metaView.setUint32(0, nameBytes.length, false)
  metaBytes.set(nameBytes, 4)
  metaView.setBigUint64(4 + nameBytes.length, BigInt(file.size), false)

  // Combine metadata + file data for encryption/signing
  const payload = new Uint8Array(metaLen + fileData.byteLength)
  payload.set(metaBytes, 0)
  payload.set(new Uint8Array(fileData), metaLen)

  let bodyToSign = payload
  let bodyToPack

  if (aesKey) {
    const { ciphertext, iv } = await aesEncrypt(aesKey, payload)
    // IV (12 bytes) + ciphertext
    bodyToPack = new Uint8Array(12 + ciphertext.length)
    bodyToPack.set(iv, 0)
    bodyToPack.set(ciphertext, 12)
  } else {
    bodyToPack = payload
  }

  // Sign the encrypted body (or plain if not encrypted)
  if (signingKey) {
    const sig = await signData(signingKey.privateKey, bodyToPack)
    const sigHeader = new Uint8Array(2 + sig.length)
    const sigView = new DataView(sigHeader.buffer)
    sigView.setUint16(0, sig.length, false)
    sigHeader.set(sig, 2)
    headerParts.push(sigHeader)
  }

  // Assemble final package
  const totalLen = headerParts.reduce((sum, h) => sum + h.length, 0) + bodyToPack.length
  const result = new Uint8Array(totalLen)
  let offset = 0
  for (const part of headerParts) {
    result.set(part, offset)
    offset += part.length
  }
  result.set(bodyToPack, offset)

  return result
}

/**
 * Parse a .wlp offline package.
 *
 * @param {Uint8Array} data - The .wlp package bytes
 * @returns {object} - { flags, groupId, signature, iv, ciphertext, metadata }
 */
export function parseWlp(data) {
  if (data.length < 13) throw new Error('无效的 .wlp 文件：文件过短')
  if (!isWlpMagic(data)) throw new Error('无效的 .wlp 文件：魔数不匹配')

  let offset = 4
  const flags = data[offset++]

  const groupBound = !!(flags & 1)
  const aesEncrypted = !!(flags & 2)
  const signed = !!(flags & 4)

  // Group ID
  const groupIdView = new DataView(data.buffer, data.byteOffset + offset, 8)
  const groupId = groupBound ? Number(groupIdView.getBigUint64(0, false)) : 0
  offset += 8

  // Signature
  let signature = null
  if (signed) {
    const sigView = new DataView(data.buffer, data.byteOffset + offset, 2)
    const sigLen = sigView.getUint16(0, false)
    offset += 2
    signature = data.slice(offset, offset + sigLen)
    offset += sigLen
  }

  // Body
  let iv = null
  let ciphertext = null
  let payload

  if (aesEncrypted) {
    iv = data.slice(offset, offset + 12)
    offset += 12
    ciphertext = data.slice(offset)
    payload = null // needs decryption
  } else {
    payload = data.slice(offset)
  }

  return { flags, groupBound, aesEncrypted, signed, groupId, signature, iv, ciphertext, payload }
}

/**
 * Extract metadata from decrypted payload.
 * Returns { fileName, fileSize, fileData }.
 */
export function extractPayloadData(payload) {
  const view = new DataView(payload.buffer, payload.byteOffset, payload.length)
  const nameLen = view.getUint32(0, false)
  const decoder = new TextDecoder()
  const fileName = decoder.decode(payload.slice(4, 4 + nameLen))
  const fileSize = Number(view.getBigUint64(4 + nameLen, false))
  const fileData = payload.slice(4 + nameLen + 8)

  return { fileName, fileSize, fileData }
}

/**
 * Build a .wlk key file.
 *
 * Format:
 * [4 bytes magic "WLK\1"]
 * [4 bytes keyLen (big-endian)]
 * [keyLen bytes AES key raw]
 * [8 bytes expiresAt (big-endian, epoch ms)]
 */
export async function buildWlk(aesKey, ttlSeconds) {
  const keyRaw = await exportAesKey(aesKey)
  // ttlSeconds === 0 means permanent (expiresAt = 0)
  const expiresAt = ttlSeconds > 0 ? Date.now() + ttlSeconds * 1000 : 0
  const totalLen = 4 + 4 + keyRaw.length + 8
  const result = new Uint8Array(totalLen)

  result.set(WLK_MAGIC, 0)
  const view = new DataView(result.buffer)
  view.setUint32(4, keyRaw.length, false)
  result.set(keyRaw, 8)
  view.setBigUint64(8 + keyRaw.length, BigInt(expiresAt), false)

  return result
}

/**
 * Parse a .wlk key file.
 *
 * @param {Uint8Array} data
 * @returns {{ aesKey: CryptoKey, expiresAt: number }}
 */
export async function parseWlk(data) {
  if (data.length < 20) throw new Error('无效的 .wlk 文件：文件过短')

  const magicOk = data[0] === WLK_MAGIC[0] && data[1] === WLK_MAGIC[1] &&
                  data[2] === WLK_MAGIC[2] && data[3] === WLK_MAGIC[3]
  if (!magicOk) throw new Error('无效的 .wlk 文件：魔数不匹配')

  const view = new DataView(data.buffer, data.byteOffset, data.length)
  const keyLen = view.getUint32(4, false)
  const keyRaw = data.slice(8, 8 + keyLen)
  const expiresAt = Number(view.getBigUint64(8 + keyLen, false))

  // expiresAt === 0 means permanent (no expiry)
  if (expiresAt > 0 && Date.now() > expiresAt) {
    throw new Error('密钥已过期')
  }

  const aesKey = await importAesKey(keyRaw)
  return { aesKey, expiresAt, permanent: expiresAt === 0 }
}

function isWlpMagic(data) {
  return data[0] === WLP_MAGIC[0] && data[1] === WLP_MAGIC[1] &&
         data[2] === WLP_MAGIC[2] && data[3] === WLP_MAGIC[3]
}
