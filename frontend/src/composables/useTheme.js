import { ref, computed, watch } from 'vue'

function readInitial() {
  const saved = localStorage.getItem('theme')
  if (saved === 'light' || saved === 'dark') {
    return saved
  }
  return 'dark'
}

const theme = ref(readInitial())
const isDark = computed(() => theme.value === 'dark')

function toggle() {
  if (theme.value === 'dark') {
    theme.value = 'light'
  } else {
    theme.value = 'dark'
  }
}

watch(theme, (val) => {
  localStorage.setItem('theme', val)
  document.documentElement.setAttribute('data-theme', val)
}, { immediate: true })

export function useTheme() {
  return { theme, isDark, toggle }
}
