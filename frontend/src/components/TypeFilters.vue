<template>
  <div class="type-filters">

    <div class="section-label" @click="collapsed = !collapsed">
      <span class="chevron">{{ collapsed ? '▶' : '▼' }}</span>
      Type Filters
    </div>

    <template v-if="!collapsed">
    <div class="panel-content">

    <div class="filter-row">
      <span class="row-label">Rarity</span>
      <div class="rarity-btns">
        <button type="button" class="rarity-btn rarity-normal" :class="{ active: rarity === 'NORMAL' }" @click="toggleRarity('NORMAL')">Normal</button>
        <button type="button" class="rarity-btn rarity-magic"  :class="{ active: rarity === 'MAGIC'  }" @click="toggleRarity('MAGIC')">Magic</button>
        <button type="button" class="rarity-btn rarity-rare"   :class="{ active: rarity === 'RARE'   }" @click="toggleRarity('RARE')">Rare</button>
        <button type="button" class="rarity-btn rarity-unique"    :class="{ active: rarity === 'UNIQUE'    }" @click="toggleRarity('UNIQUE')">Unique</button>
        <button type="button" class="rarity-btn rarity-nonunique" :class="{ active: rarity === 'NONUNIQUE' }" @click="toggleRarity('NONUNIQUE')">Non-Unique</button>
      </div>
    </div>

    <div class="filter-row">
      <span class="row-label">Category</span>
      <div class="autocomplete">
        <input
          type="text"
          :value="categorySearch"
          placeholder="Any category..."
          autocomplete="off"
          @focus="onFocus"
          @blur="onBlur"
          @input="onInput"
          @keydown="onCategoryKeydown"
        />
        <button v-if="category" type="button" class="clear-btn" @mousedown.prevent="clearCategory">×</button>
        <ul v-if="showDropdown && dropdownItems.length" class="dropdown">
          <template v-for="item in dropdownItems" :key="item.isHeader ? 'h:' + item.label : item.value">
            <li v-if="item.isHeader" class="dropdown-header">{{ item.label }}</li>
            <li
              v-else
              class="dropdown-item"
              :class="{ selected: item.value === category }"
              @mousedown.prevent="selectCategory(item)"
            >
              <span class="cat-label">{{ item.label }}</span>
              <span class="cat-api">{{ item.value }}</span>
            </li>
          </template>
        </ul>
      </div>
    </div>

    </div>
    </template>

  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const CATEGORIES = [
  { label: 'Body Armour',     value: 'armour.chest',        group: 'Armour' },
  { label: 'Helmet',          value: 'armour.helmet',       group: 'Armour' },
  { label: 'Gloves',          value: 'armour.gloves',       group: 'Armour' },
  { label: 'Boots',           value: 'armour.boots',        group: 'Armour' },
  { label: 'Shield',          value: 'armour.shield',       group: 'Armour' },
  { label: 'Focus',           value: 'armour.focus',        group: 'Armour' },
  { label: 'Quiver',          value: 'armour.quiver',       group: 'Armour' },
  { label: 'Wand',            value: 'weapon.wand',         group: 'One Hand Weapon' },
  { label: 'Sceptre',         value: 'weapon.sceptre',      group: 'One Hand Weapon' },
  { label: 'Claw',            value: 'weapon.claw',         group: 'One Hand Weapon' },
  { label: 'Dagger',          value: 'weapon.dagger',       group: 'One Hand Weapon' },
  { label: 'Rune Dagger',     value: 'weapon.runedagger',   group: 'One Hand Weapon' },
  { label: 'One Hand Sword',  value: 'weapon.onesword',     group: 'One Hand Weapon' },
  { label: 'One Hand Axe',    value: 'weapon.oneaxe',       group: 'One Hand Weapon' },
  { label: 'One Hand Mace',   value: 'weapon.onemace',      group: 'One Hand Weapon' },
  { label: 'Flail',           value: 'weapon.flail',        group: 'One Hand Weapon' },
  { label: 'Spear',           value: 'weapon.spear',        group: 'One Hand Weapon' },
  { label: 'Staff',           value: 'weapon.staff',        group: 'Two Hand Weapon' },
  { label: 'Warstaff',        value: 'weapon.warstaff',     group: 'Two Hand Weapon' },
  { label: 'Bow',             value: 'weapon.bow',          group: 'Two Hand Weapon' },
  { label: 'Two Hand Sword',  value: 'weapon.twosword',     group: 'Two Hand Weapon' },
  { label: 'Two Hand Axe',    value: 'weapon.twoaxe',       group: 'Two Hand Weapon' },
  { label: 'Two Hand Mace',   value: 'weapon.twomace',      group: 'Two Hand Weapon' },
  { label: 'Crossbow',        value: 'weapon.crossbow',     group: 'Two Hand Weapon' },
  { label: 'Quarterstaff',    value: 'weapon.quarterstaff', group: 'Two Hand Weapon' },
  { label: 'Amulet',          value: 'accessory.amulet',    group: 'Accessory' },
  { label: 'Ring',            value: 'accessory.ring',      group: 'Accessory' },
  { label: 'Belt',            value: 'accessory.belt',      group: 'Accessory' },
  { label: 'Jewel',           value: 'jewel.base',          group: 'Other' },
  { label: 'Abyss Jewel',     value: 'jewel.abyss',         group: 'Other' },
  { label: 'Cluster Jewel',   value: 'jewel.cluster',       group: 'Other' },
  { label: 'Flask',           value: 'flask',               group: 'Other' },
  { label: 'Skill Gem',       value: 'gem.activegem',       group: 'Other' },
  { label: 'Support Gem',     value: 'gem.supportgem',      group: 'Other' },
]

const props = defineProps({
  rarity:   { type: String, default: null },
  category: { type: String, default: null },
})
const emit = defineEmits(['update:rarity', 'update:category'])

const collapsed = ref(false)
const categorySearch = ref('')
const showDropdown = ref(false)

watch(() => props.category, (val) => {
  if (!val) categorySearch.value = ''
})

const filteredCategories = computed(() => {
  const q = categorySearch.value.trim().toLowerCase()
  if (!q) return CATEGORIES
  const words = q.split(/\s+/).filter(Boolean)
  return CATEGORIES.filter(cat => {
    const haystack = (cat.label + ' ' + cat.value + ' ' + cat.group).toLowerCase()
    return words.every(w => haystack.includes(w))
  })
})

const dropdownItems = computed(() => {
  const items = []
  let lastGroup = null
  for (const cat of filteredCategories.value) {
    if (cat.group !== lastGroup) {
      items.push({ isHeader: true, label: cat.group })
      lastGroup = cat.group
    }
    items.push({ isHeader: false, ...cat })
  }
  return items
})

function toggleRarity(value) {
  emit('update:rarity', props.rarity === value ? null : value)
}

function onCategoryKeydown(e) {
  if (e.key === 'Tab' && showDropdown.value && filteredCategories.value.length) {
    e.preventDefault()
    selectCategory(filteredCategories.value[0])
  }
}

function onFocus() {
  showDropdown.value = true
}

function onBlur() {
  setTimeout(() => { showDropdown.value = false }, 150)
}

function onInput(e) {
  categorySearch.value = e.target.value
  emit('update:category', null)
  showDropdown.value = true
}

function selectCategory(cat) {
  categorySearch.value = cat.label
  emit('update:category', cat.label)
  showDropdown.value = false
}

function clearCategory() {
  categorySearch.value = ''
  emit('update:category', null)
}
</script>

<style scoped>
.type-filters {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.panel-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 28px;
}

.section-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-label);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--color-border-light);
  padding-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
}

.section-label:hover {
  color: var(--color-text-secondary);
}

.chevron {
  font-size: 9px;
  color: var(--color-text-faint);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.row-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-label);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  width: 62px;
  flex-shrink: 0;
}

/* Rarity */
.rarity-btns {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.rarity-btn {
  padding: 4px 12px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 5px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: opacity 0.12s, border-color 0.12s;
  opacity: 0.5;
}

.rarity-btn.active {
  opacity: 1;
  border-color: rgba(255,255,255,0.6);
}

.rarity-normal { background: #2b2b2b; color: #c8c8c8; }
.rarity-magic  { background: #16165c; color: #8888ff; }
.rarity-rare   { background: #3c3100; color: #ffff77; }
.rarity-unique    { background: #2d1500; color: #af6025; }
.rarity-nonunique { background: #1a1a2e; color: #9e9eb8; }

/* Category autocomplete */
.autocomplete {
  position: relative;
  flex: 1;
}

input[type="text"] {
  width: 100%;
  padding: 7px 28px 7px 10px;
  font-size: 14px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  background: var(--color-surface);
  color: var(--color-text);
  box-sizing: border-box;
}

input[type="text"]:focus {
  border-color: var(--color-accent);
}

.clear-btn {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 16px;
  color: var(--color-text-faint);
  cursor: pointer;
  padding: 0 2px;
  line-height: 1;
}

.clear-btn:hover { color: var(--color-text-secondary); }

.dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 6px;
  box-shadow: 0 4px 12px var(--color-shadow);
  list-style: none;
  margin: 0;
  padding: 4px 0;
  z-index: 100;
  max-height: 280px;
  overflow-y: auto;
}

.dropdown-header {
  padding: 6px 12px 3px;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-faint);
  pointer-events: none;
}

.dropdown-item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  transition: background 0.1s;
}

.dropdown-item:hover,
.dropdown-item.selected {
  background: var(--color-accent-soft);
}

.cat-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
}

.cat-api {
  font-size: 11px;
  color: var(--color-text-optional);
  font-family: monospace;
}
</style>
