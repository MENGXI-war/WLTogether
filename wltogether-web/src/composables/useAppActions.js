import { ref, watch } from 'vue'

const createGroupTrigger = ref(0)

export function useAppActions() {
  function requestCreateGroup() {
    createGroupTrigger.value++
  }

  function onRequestCreateGroup(fn) {
    return watch(createGroupTrigger, fn)
  }

  return { requestCreateGroup, onRequestCreateGroup }
}
