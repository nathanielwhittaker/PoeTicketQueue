<template>
  <div class="filter-group">
    <div class="group-header">
      <button type="button" class="collapse-btn" @click="collapsed = !collapsed" :title="collapsed ? 'Expand' : 'Collapse'">{{ collapsed ? '▶' : '▼' }}</button>
      <div class="type-tabs">
        <button
          v-for="t in ['and', 'not', 'count']"
          :key="t"
          type="button"
          :class="['type-tab', { active: group.type === t }]"
          @click="group.type = t"
        >{{ t }}</button>
      </div>
      <div v-if="group.type === 'count'" class="count-range">
        <span class="count-label">Count</span>
        <input v-model.number="group.countMin" type="number" placeholder="Min" class="count-input" />
        <span class="range-sep">–</span>
        <input v-model.number="group.countMax" type="number" placeholder="Max" class="count-input" />
      </div>
      <button type="button" class="btn-icon header-remove" @click="$emit('remove')" title="Remove group">✕</button>
    </div>

    <template v-if="!collapsed">
    <div class="stat-rows">
      <div v-if="group.filters.length === 0" class="no-stats">No stats added yet.</div>
      <div v-for="(filter, fi) in group.filters" :key="filter.id" class="stat-row">
        <div class="autocomplete">
          <input
            v-model="filter.text"
            type="text"
            placeholder="Stat name..."
            class="stat-search"
            autocomplete="off"
            @focus="focusedId = filter.id"
            @blur="onBlur"
            @keydown="e => onStatKeydown(filter, e)"
          />
          <ul v-if="focusedId === filter.id && suggestionsFor(filter).length" class="dropdown">
            <li
              v-for="s in suggestionsFor(filter)"
              :key="s.id"
              class="dropdown-item"
              @mousedown.prevent="selectStat(filter, s)"
            >
              <span class="stat-type" :style="{ background: TYPE_COLORS[s.id.split('.')[0]] || '#7a8a9a' }">{{ s.id.split('.')[0] }}</span>
              <span class="stat-text">{{ s.text }}</span>
            </li>
          </ul>
        </div>
        <input v-model.number="filter.min" type="number" placeholder="Min" class="stat-val" />
        <input v-model.number="filter.max" type="number" placeholder="Max" class="stat-val" />
        <button type="button" class="btn-icon" @click="removeStat(fi)" title="Remove stat">✕</button>
      </div>
    </div>

    <button type="button" class="btn-add-stat" @click="addStat">+ Add Stat</button>
    </template>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

let nextId = 0
const newFilter = () => ({ id: nextId++, text: '', statId: null, min: null, max: null })

const props = defineProps({
  group: { type: Object, required: true },
  stats: { type: Array, default: () => [] },
})

defineEmits(['remove'])

props.group.filters.push(newFilter())

const collapsed = ref(false)
const focusedId = ref(null)

const STAT_TYPES = ['pseudo', 'explicit', 'implicit', 'enchant', 'fractured', 'crafted', 'veiled', 'monster']

const TYPE_COLORS = {
  pseudo:    '#7a8a9a',
  enchant:   '#e91e8c',
  crafted:   '#5bb8d4',
  explicit:  '#1a1a1a',
  implicit:  '#c0392b',
  fractured: '#c4a882',
  veiled:    '#2e7d32',
  monster:   '#e65100',
}

function suggestionsFor(filter) {
  const words = filter.text.trim().toLowerCase().split(/\s+/).filter(w => w.length > 0)
  if (words.length === 0) return []

  let typeFilter = null
  const searchWords = []
  for (const w of words) {
    const matched = w.length >= 2 && !typeFilter && STAT_TYPES.find(t => t.startsWith(w))
    if (matched) typeFilter = matched
    else searchWords.push(w)
  }

  if (!typeFilter && (searchWords.length === 0 || searchWords[0].length < 2)) return []

  const prefix = typeFilter ? typeFilter + '.' : null
  const startsWith = []
  const contains = []
  for (const s of props.stats) {
    if (prefix && !s.id.startsWith(prefix)) continue
    if (searchWords.length > 0) {
      const t = s.text.toLowerCase()
      if (!searchWords.every(w => t.includes(w))) continue
      if (t.startsWith(searchWords[0])) startsWith.push(s)
      else contains.push(s)
    } else {
      contains.push(s)
    }
    if (startsWith.length + contains.length >= 10) break
  }
  return [...startsWith, ...contains].slice(0, 10)
}

function onStatKeydown(filter, e) {
  if (e.key !== 'Tab') return
  const sug = suggestionsFor(filter)
  if (focusedId.value === filter.id && sug.length > 0) {
    e.preventDefault()
    selectStat(filter, sug[0])
  }
}

function selectStat(filter, stat) {
  filter.text = stat.text
  filter.statId = stat.id
  focusedId.value = null
}

function onBlur() {
  setTimeout(() => { focusedId.value = null }, 150)
}

function addStat() {
  props.group.filters.push(newFilter())
}

function removeStat(fi) {
  props.group.filters.splice(fi, 1)
}
</script>

<style scoped>
.filter-group {
  border: 1px solid #ddd;
  border-radius: 8px;
  background: #fafafa;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #f0f0f0;
  border-bottom: 1px solid #ddd;
  border-radius: 8px 8px 0 0;
}

.type-tabs {
  display: flex;
  border: 1px solid #ccc;
  border-radius: 5px;
  overflow: hidden;
  flex-shrink: 0;
}

.type-tab {
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border: none;
  background: #fff;
  color: #888;
  cursor: pointer;
  transition: background 0.12s, color 0.12s;
}

.type-tab + .type-tab {
  border-left: 1px solid #ccc;
}

.type-tab.active {
  background: #4a90e2;
  color: #fff;
}

.type-tab:hover:not(.active) {
  background: #e8e8e8;
  color: #555;
}

.count-range {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.count-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
}

input.count-input {
  width: 55px;
  text-align: center;
}

.range-sep {
  color: #aaa;
  font-size: 14px;
}

.collapse-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 9px;
  color: #aaa;
  padding: 4px 4px;
  line-height: 1;
  flex-shrink: 0;
}

.collapse-btn:hover {
  color: #555;
}

.header-remove {
  margin-left: auto;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: #aaa;
  padding: 4px 6px;
  border-radius: 4px;
  line-height: 1;
  transition: color 0.12s, background 0.12s;
  flex-shrink: 0;
}

.btn-icon:hover {
  color: #d9534f;
  background: #fdf0ef;
}

.stat-rows {
  padding: 8px 10px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.no-stats {
  font-size: 13px;
  color: #bbb;
  padding: 4px 0;
}

.stat-row {
  display: grid;
  grid-template-columns: 1fr 55px 55px auto;
  align-items: center;
  gap: 6px;
}

.autocomplete {
  position: relative;
  min-width: 0;
}

.dropdown {
  position: absolute;
  top: calc(100% + 3px);
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  list-style: none;
  margin: 0;
  padding: 4px 0;
  z-index: 100;
  max-height: 220px;
  overflow-y: auto;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  transition: background 0.1s;
  overflow: hidden;
}

.dropdown-item:hover {
  background: #f0f5fc;
}

.stat-type {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #fff;
  padding: 1px 6px;
  border-radius: 3px;
  flex-shrink: 0;
}

.stat-text {
  font-size: 13px;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

input[type="text"],
input[type="number"] {
  padding: 8px 10px;
  font-size: 14px;
  border: 1px solid #ccc;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  background: #fff;
  box-sizing: border-box;
  width: 100%;
}

input[type="text"]:focus,
input[type="number"]:focus {
  border-color: #4a90e2;
}

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
  -webkit-appearance: none;
}
input[type="number"] { -moz-appearance: textfield; }

input.stat-val {
  width: 55px;
  flex-shrink: 0;
  text-align: center;
}

.btn-add-stat {
  margin: 0 10px 10px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #4a90e2;
  background: none;
  border: 1px dashed #4a90e2;
  border-radius: 5px;
  cursor: pointer;
  transition: background 0.12s;
  display: block;
}

.btn-add-stat:hover {
  background: #eef4fc;
}
</style>
