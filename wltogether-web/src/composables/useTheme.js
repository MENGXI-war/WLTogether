import { ref } from 'vue'

const THEME_KEY = 'theme'

// Reactive state shared across components
const currentTheme = ref(loadTheme())
const isDark = ref(false)

// Single system preference MediaQueryList — reused across all 'auto' calls
const systemMq = window.matchMedia('(prefers-color-scheme: dark)')
let systemChangeHandler = null

function loadTheme() {
  return localStorage.getItem(THEME_KEY) || 'auto'
}

function applyTheme(value) {
  // Remove old system listener if any
  if (systemChangeHandler) {
    systemMq.removeEventListener('change', systemChangeHandler)
    systemChangeHandler = null
  }

  if (value === 'dark') {
    document.documentElement.classList.add('dark')
    isDark.value = true
  } else if (value === 'light') {
    document.documentElement.classList.remove('dark')
    isDark.value = false
  } else {
    // 'auto' — follow system preference
    const apply = () => {
      if (systemMq.matches) {
        document.documentElement.classList.add('dark')
        isDark.value = true
      } else {
        document.documentElement.classList.remove('dark')
        isDark.value = false
      }
    }
    apply()
    // Store handler reference so we can remove it later
    systemChangeHandler = apply
    systemMq.addEventListener('change', apply)
  }
}

function saveTheme(value) {
  localStorage.setItem(THEME_KEY, value)
  currentTheme.value = value
  applyTheme(value)
}

/**
 * Initialize theme on app startup.
 * Also listens for system preference changes when in 'auto' mode.
 */
export function useTheme() {
  // Apply on first call (synchronous, before mount)
  applyTheme(currentTheme.value)

  return {
    currentTheme,
    isDark,
    setTheme: saveTheme
  }
}
