import { defineStore } from 'pinia'
import { ref } from 'vue'
import groupsApi from '@/api/groups'
import sessionsApi from '@/api/sessions'

export const useGroupsStore = defineStore('groups', () => {
  const groups = ref([])
  const currentGroup = ref(null)
  const members = ref([])
  const sessions = ref([])
  const loading = ref(false)

  async function fetchGroups() {
    loading.value = true
    try {
      const res = await groupsApi.list()
      groups.value = res
    } finally {
      loading.value = false
    }
  }

  async function fetchGroup(id) {
    loading.value = true
    try {
      const group = await groupsApi.get(id)
      currentGroup.value = group
      return group
    } finally {
      loading.value = false
    }
  }

  async function createGroup(data) {
    const group = await groupsApi.create(data)
    groups.value.unshift(group)
    return group
  }

  async function updateGroup(id, data) {
    const group = await groupsApi.update(id, data)
    if (currentGroup.value?.id == id) {
      currentGroup.value = group
    }
    const idx = groups.value.findIndex(g => g.id == id)
    if (idx >= 0) groups.value[idx] = group
    return group
  }

  async function deleteGroup(id) {
    await groupsApi.remove(id)
    groups.value = groups.value.filter(g => g.id != id)
    if (currentGroup.value?.id == id) {
      currentGroup.value = null
    }
  }

  async function fetchMembers(groupId) {
    members.value = await groupsApi.listMembers(groupId)
  }

  async function inviteMember(groupId, username) {
    const member = await groupsApi.inviteMember(groupId, username)
    members.value.push(member)
    return member
  }

  async function removeMember(groupId, userId) {
    await groupsApi.removeMember(groupId, userId)
    members.value = members.value.filter(m => m.userId != userId)
  }

  async function leaveGroup(groupId) {
    await groupsApi.leave(groupId)
    groups.value = groups.value.filter(g => g.id != groupId)
    if (currentGroup.value?.id == groupId) {
      currentGroup.value = null
    }
  }

  async function changeMemberRole(groupId, userId, role) {
    await groupsApi.changeMemberRole(groupId, userId, role)
    const m = members.value.find(m => m.userId == userId)
    if (m) m.role = role
  }

  async function transferOwner(groupId, newOwnerId) {
    await groupsApi.transferOwner(groupId, newOwnerId)
  }

  async function muteMember(groupId, userId, duration) {
    await groupsApi.muteMember(groupId, userId, duration)
  }

  async function updateNickname(groupId, nicknameInGroup) {
    await groupsApi.updateNickname(groupId, nicknameInGroup)
  }

  async function uploadAvatar(groupId, file) {
    const res = await groupsApi.uploadAvatar(groupId, file)
    if (currentGroup.value?.id == groupId) {
      currentGroup.value.avatarUrl = res.data || res
    }
    return res
  }

  async function deleteAvatar(groupId) {
    await groupsApi.deleteAvatar(groupId)
    if (currentGroup.value?.id == groupId) {
      currentGroup.value.avatarUrl = null
    }
  }

  async function fetchSessions(groupId) {
    sessions.value = await sessionsApi.list(groupId)
  }

  async function createSession(groupId, data) {
    const session = await sessionsApi.create(groupId, data)
    sessions.value.unshift(session)
    return session
  }

  function clearCurrent() {
    currentGroup.value = null
    members.value = []
    sessions.value = []
  }

  return {
    groups,
    currentGroup,
    members,
    sessions,
    loading,
    fetchGroups,
    fetchGroup,
    createGroup,
    updateGroup,
    deleteGroup,
    fetchMembers,
    inviteMember,
    removeMember,
    leaveGroup,
    changeMemberRole,
    transferOwner,
    muteMember,
    updateNickname,
    uploadAvatar,
    deleteAvatar,
    fetchSessions,
    createSession,
    clearCurrent
  }
})
