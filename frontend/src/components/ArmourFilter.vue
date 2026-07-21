<template>
  <div class="armour-filter">
    <span class="armour-filter-label">{{ label }}</span>
    <input
      :value="modelValue.min"
      type="number"
      min="0"
      placeholder="min"
      class="armour-filter-input"
      @input="emit('update:modelValue', { ...modelValue, min: toNum($event.target.value) })"
    />
    <input
      :value="modelValue.max"
      type="number"
      min="0"
      placeholder="max"
      class="armour-filter-input"
      @input="emit('update:modelValue', { ...modelValue, max: toNum($event.target.value) })"
    />
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  modelValue: { type: Object, default: () => ({ min: null, max: null }) },
})

const emit = defineEmits(['update:modelValue'])

function toNum(val) {
  const n = parseInt(val, 10)
  return isNaN(n) ? null : n
}
</script>

<style scoped>
.armour-filter {
  display: flex;
  align-items: center;
  gap: 6px;
}

.armour-filter-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-label);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  width: 90px;
  flex-shrink: 0;
}

.armour-filter-input {
  width: 60px;
  padding: 4px 6px;
  font-size: 13px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  box-sizing: border-box;
  background: var(--color-surface);
  color: var(--color-text);
}

.armour-filter-input:focus {
  border-color: var(--color-accent);
}
</style>
