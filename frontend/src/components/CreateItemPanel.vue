<template>
  <div class="panel">
    <h2>Add Item</h2>

    <div class="field">
      <label>Item Name</label>
      <div class="autocomplete">
        <input
          v-model="itemName"
          type="text"
          placeholder="Item name..."
          autocomplete="off"
          @focus="showDropdown = true"
          @blur="onBlur"
          @input="onNameInput"
          @keydown="onNameKeydown"
        />
        <ul v-if="showDropdown && suggestions.length" class="dropdown">
          <li
            v-for="s in suggestions"
            :key="s.text"
            class="dropdown-item"
            @mousedown.prevent="selectSuggestion(s)"
          >
            <span class="suggestion-text">{{ s.text }}</span>
            <span v-if="s.detail" class="suggestion-detail">{{ s.detail }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="filters-columns">
      <div class="filter-section">
        <TypeFilters
          :rarity="itemRarity"
          :category="itemBaseType"
          @update:rarity="onRarityUpdate"
          @update:category="onCategoryUpdate"
        />
      </div>

      <div class="filter-section">
        <div class="section-label" @click="armourFiltersCollapsed = !armourFiltersCollapsed">
          <span class="chevron">{{ armourFiltersCollapsed ? '▶' : '▼' }}</span>
          Armour Filters
        </div>
        <template v-if="!armourFiltersCollapsed">
          <div class="armour-grid">
            <ArmourFilter
              v-for="f in armourFilterFields"
              :key="f.key"
              :label="f.label"
              v-model="armourFilters[f.key]"
            />
          </div>
        </template>
      </div>

      <div class="filter-section">
        <div class="section-label" @click="statFiltersCollapsed = !statFiltersCollapsed">
          <span class="chevron">{{ statFiltersCollapsed ? '▶' : '▼' }}</span>
          Stat Filters
        </div>
        <template v-if="!statFiltersCollapsed">
          <div class="groups">
            <StatFilterGroup
              v-for="(group, gi) in filterGroups"
              :key="group.id"
              :group="group"
              :stats="stats"
              @remove="removeGroup(gi)"
            />
          </div>
          <button type="button" class="btn-add-group" @click="addGroup">+ Add Group</button>
        </template>
      </div>
    </div>

    <p v-if="error" class="error">{{ error }}</p>

    <button type="button" class="btn-submit" :disabled="(!itemName && !itemBaseType) || adding" @click="handleAddItem">
      {{ adding ? 'Adding...' : 'Add to Queue' }}
    </button>

    <div class="filter-section import-build-section">
      <div class="section-label">Import Build</div>
      <div class="field">
        <input
          v-model="buildUrl"
          type="text"
          placeholder="Paste pobb.in build URL..."
          autocomplete="off"
          @input="importError = ''"
        />
      </div>
      <label class="checkbox-row">
        <input type="checkbox" v-model="useTrueValues" />
        Use true item values
      </label>
      <p v-if="importError" class="error">{{ importError }}</p>
      <button
        type="button"
        class="btn-submit"
        :disabled="!buildUrl || importing"
        @click="handleImportBuild"
      >
        {{ importing ? 'Importing...' : 'Import' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import StatFilterGroup from './StatFilterGroup.vue'
import TypeFilters from './TypeFilters.vue'
import ArmourFilter from './ArmourFilter.vue'

const props = defineProps({
  groupCode: { type: String, required: true },
  baseTypes: { type: Array, default: () => [] },
  stats: { type: Array, default: () => [] },
})

const emit = defineEmits(['item-added', 'build-imported'])

let nextId = 0
const newGroup = () => ({ id: nextId++, type: 'and', countMin: null, countMax: null, filters: [] })

const statFiltersCollapsed = ref(false)
const armourFiltersCollapsed = ref(false)
const armourFilterFields = [
  { key: 'basePercentile', label: 'Base %' },
  { key: 'ar',             label: 'Armour' },
  { key: 'ev',             label: 'Evasion' },
  { key: 'es',             label: 'Energy Shield' },
  { key: 'ward',           label: 'Ward' },
  { key: 'block',          label: 'Block' },
]
const emptyArmourFilters = () => ({
  basePercentile: { min: null, max: null },
  ar:             { min: null, max: null },
  ev:             { min: null, max: null },
  es:             { min: null, max: null },
  ward:           { min: null, max: null },
  block:          { min: null, max: null },
})
const armourFilters = ref(emptyArmourFilters())
const itemName = ref('')
const itemRarity = ref(null)
const itemBaseType = ref(null)
const filterGroups = ref([newGroup()])
const adding = ref(false)
const error = ref('')
const showDropdown = ref(false)
const buildUrl = ref('')
const useTrueValues = ref(false)
const importing = ref(false)
const importError = ref('')

const suggestions = computed(() => {
  const q = itemName.value.trim().toLowerCase()
  if (q.length < 2) return []
  const startsWith = []
  const contains = []
  for (const entry of props.baseTypes) {
    const t = entry.text.toLowerCase()
    if (t.startsWith(q)) startsWith.push(entry)
    else if (t.includes(q)) contains.push(entry)
    if (startsWith.length + contains.length >= 10) break
  }
  return [...startsWith, ...contains].slice(0, 10)
})

function onRarityUpdate(val) { itemRarity.value = val }
function onCategoryUpdate(val) { itemBaseType.value = val }

function onNameKeydown(e) {
  if (e.key === 'Tab' && showDropdown.value && suggestions.value.length) {
    e.preventDefault()
    selectSuggestion(suggestions.value[0])
  }
}

function onNameInput() {
  // Only clear UNIQUE (set by autocomplete); manual rarity overrides survive name edits.
  if (itemRarity.value === 'UNIQUE') {
    itemRarity.value = null
  }
}

function selectSuggestion(s) {
  itemName.value = s.text
  itemRarity.value = s.unique ? 'UNIQUE' : null
  itemBaseType.value = s.unique ? s.detail : null
  showDropdown.value = false
}

function resolveRarity() {
  if (itemRarity.value) return itemRarity.value

  const match = props.baseTypes.find(
    e => e.text.toLowerCase() === itemName.value.trim().toLowerCase()
  )
  if (match?.unique) return 'UNIQUE'

  const nonBaseCount = filterGroups.value
    .flatMap(g => g.filters)
    .filter(f => f.text && f.text.trim() !== '')
    .filter(f => !f.statId || (!f.statId.startsWith('enchant.') && !f.statId.startsWith('implicit.')))
    .length

  return nonBaseCount >= 1 ? 'RARE' : 'NORMAL'
}

function onBlur() {
  setTimeout(() => { showDropdown.value = false }, 150)
}

function addGroup() {
  filterGroups.value.push(newGroup())
}

function removeGroup(gi) {
  filterGroups.value.splice(gi, 1)
}

async function handleAddItem() {
  error.value = ''
  adding.value = true
  try {
    const rarity = resolveRarity()
    const isUnique = rarity === 'UNIQUE'
    const response = await fetch(`/api/groups/${props.groupCode}/items`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: isUnique ? itemName.value : null,
        rarity,
        baseType: isUnique ? itemBaseType.value : itemName.value,
        filterGroups: filterGroups.value.map(g => ({
          type: g.type,
          countMin: g.countMin,
          countMax: g.countMax,
          filters: g.filters.map(f => ({
            statId: f.statId,
            text: f.text,
            min: f.min ?? null,
            max: f.max ?? null,
          })),
        })),
        armourFilters: Object.fromEntries(
          Object.entries(armourFilters.value)
            .filter(([, v]) => v.min !== null || v.max !== null)
        ),
      }),
    })
    if (!response.ok) throw new Error('Failed to add item')
    const updated = await response.json()
    emit('item-added', updated.itemQueue)
    itemName.value = ''
    itemRarity.value = null
    itemBaseType.value = null
    filterGroups.value = [newGroup()]
    armourFilters.value = emptyArmourFilters()
  } catch (e) {
    error.value = e.message
  } finally {
    adding.value = false
  }
}

async function handleImportBuild() {
  importError.value = ''
  importing.value = true
  try {
    const res = await fetch(`/api/groups/${props.groupCode}/builds`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ url: buildUrl.value, useTrueValues: useTrueValues.value }),
    })
    if (res.ok) {
      const updated = await res.json()
      emit('build-imported', updated)
      buildUrl.value = ''
      useTrueValues.value = false
    } else {
      const body = await res.json().catch(() => ({}))
      importError.value = body.message || 'Import failed.'
    }
  } catch (e) {
    importError.value = 'Import failed. Check your connection and try again.'
  } finally {
    importing.value = false
  }
}
</script>

<style scoped>
.panel {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}

h2 {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #555;
  margin: 0;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: #666;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.section-label:hover {
  color: #444;
}

.chevron {
  font-size: 9px;
  color: #aaa;
}

input[type="text"] {
  width: 100%;
  padding: 8px 10px;
  font-size: 14px;
  border: 1px solid #ccc;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  background: #fff;
  box-sizing: border-box;
}

input[type="text"]:focus {
  border-color: #4a90e2;
}

.autocomplete {
  position: relative;
  width: 100%;
}

.dropdown {
  position: absolute;
  top: calc(100% + 4px);
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
  max-height: 260px;
  overflow-y: auto;
}

.dropdown-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 7px 12px;
  cursor: pointer;
  transition: background 0.1s;
}

.dropdown-item:hover {
  background: #f0f5fc;
}

.suggestion-text {
  font-size: 14px;
  font-weight: 500;
  color: #222;
}

.suggestion-detail {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.filters-columns {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
  align-items: start;
}

.filter-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.armour-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 8px 16px;
}

.groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-add-group {
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  background: none;
  border: 1px dashed #bbb;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.12s, border-color 0.12s;
  align-self: flex-start;
}

.btn-add-group:hover {
  background: #f0f0f0;
  border-color: #999;
}

.checkbox-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  text-transform: none;
  font-weight: 400;
}

.error {
  color: #d9534f;
  font-size: 13px;
  margin: 0;
}

.btn-submit {
  padding: 10px 16px;
  font-size: 15px;
  font-weight: 600;
  color: white;
  background: #4a90e2;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  align-self: flex-start;
}

.btn-submit:hover:not(:disabled) {
  background: #357abd;
}

.btn-submit:disabled {
  background: #a0bde0;
  cursor: not-allowed;
}
</style>
