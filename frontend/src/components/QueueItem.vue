<template>
  <div class="item">
    <div class="main-row" :class="rarityClass">
      <button class="toggle" @click="expanded = !expanded" :title="expanded ? 'Collapse' : 'Expand'">
        {{ expanded ? '▲' : '▼' }}
      </button>
      <span class="item-name">{{ item.name || item.baseType || 'Item' }}</span>
      <span v-if="item.queuedBy" class="queued-by">{{ item.queuedBy }}</span>
      <div v-if="canTrade" class="actions">
        <button class="action bought" @click="$emit('bought', item)" title="Mark as bought">✓</button>
        <button class="action reject" @click="$emit('reject', item)" title="Reject">✕</button>
        <button class="action buy" @click="$emit('buy', item)">Buy</button>
      </div>
    </div>
    <div v-if="expanded" class="stats">
      <div v-if="defenses.length > 0" class="defenses">
        <span v-for="d in defenses" :key="d.label" class="defense-chip">
          <span class="defense-label">{{ d.label }}</span>
          <span class="defense-value">{{ d.max ? `${d.min}–${d.max}` : d.min }}</span>
        </span>
      </div>
      <template v-if="item.statGroups && item.statGroups.length > 0">
        <div v-for="(group, gi) in item.statGroups" :key="gi" class="stat-group">
          <div v-if="groupLabel(group)" class="group-label">{{ groupLabel(group) }}</div>
          <div v-for="(stat, si) in group.filters" :key="si" class="stat-row" :class="{ indented: !!groupLabel(group) }">
            <span class="stat-text">{{ formatStat(stat) }}</span>
          </div>
        </div>
      </template>
      <template v-else-if="item.stats && item.stats.length > 0">
        <div class="stat-group">
          <div v-for="(stat, i) in item.stats" :key="i" class="stat-row">
            <span class="stat-text">{{ formatStat(stat) }}</span>
          </div>
        </div>
      </template>
      <p v-else class="stats-empty">No stats.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  item: Object,
  canTrade: { type: Boolean, default: false },
})
defineEmits(['bought', 'reject', 'buy'])

const expanded = ref(true)

function formatStat(stat) {
  if (!stat.text || !stat.text.includes('#')) { return stat.text }
  const hasMin = stat.roll !== -1
  const hasMax = stat.maxRoll !== -1
  if (!hasMin && !hasMax) { return stat.text }
  const value = hasMin && hasMax ? `${stat.roll}–${stat.maxRoll}`
              : hasMin            ? `${stat.roll}`
              :                     `${stat.maxRoll}`
  return stat.text.replace('#', value)
}

function groupLabel(group) {
  if (group.type === 'not') { return 'None of:' }
  if (group.type === 'count') {
    const min = group.countMin
    const max = group.countMax
    if (min != null && max != null) { return `Between ${min}–${max} of:` }
    if (min != null) { return `At least ${min} of:` }
    if (max != null) { return `At most ${max} of:` }
    return 'Count:'
  }
  if (group.type === 'and') { return 'All of:' }
  return null
}

const DEFENSE_FIELDS = [
  { key: 'armour',        maxKey: 'maxArmour',        label: 'Armour' },
  { key: 'evasion',       maxKey: 'maxEvasion',        label: 'Evasion' },
  { key: 'es',            maxKey: 'maxEs',             label: 'ES' },
  { key: 'ward',          maxKey: 'maxWard',           label: 'Ward' },
  { key: 'block',         maxKey: 'maxBlock',          label: 'Block' },
  { key: 'basePercentile',maxKey: 'maxBasePercentile', label: 'Base %' },
]

const defenses = computed(() =>
  DEFENSE_FIELDS
    .filter(f => props.item[f.key] > 0)
    .map(f => ({ label: f.label, min: props.item[f.key], max: props.item[f.maxKey] > 0 ? props.item[f.maxKey] : null }))
)

const rarityClass = computed(() => {
  switch ((props.item.rarity || '').toUpperCase()) {
    case 'UNIQUE':    return 'rarity-unique'
    case 'RARE':      return 'rarity-rare'
    case 'MAGIC':     return 'rarity-magic'
    case 'NORMAL':    return 'rarity-normal'
    case 'NONUNIQUE': return 'rarity-nonunique'
    default:          return 'rarity-none'
  }
})
</script>

<style scoped>
.item {
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: hidden;
  width: 100%;
}

.main-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
}

.rarity-none {
  background: #fff;
}
.rarity-none .item-name { color: #222; }
.rarity-none .toggle { color: #aaa; }
.rarity-none .toggle:hover { color: #555; }

.rarity-normal {
  background: #2b2b2b;
}
.rarity-normal .item-name { color: #c8c8c8; }

.rarity-magic {
  background: #16165c;
}
.rarity-magic .item-name { color: #8888ff; }

.rarity-rare {
  background: #3c3100;
}
.rarity-rare .item-name { color: #ffff77; }

.rarity-unique {
  background: #2d1500;
}
.rarity-unique .item-name { color: #af6025; }

.rarity-nonunique {
  background: linear-gradient(90deg, #2b2b2b 0%, #16165c 40%, #3c3100 100%);
}
.rarity-nonunique .item-name { color: #b0b0c8; }

.toggle {
  font-size: 10px;
  color: rgba(255,255,255,0.35);
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
  flex-shrink: 0;
}

.toggle:hover {
  color: rgba(255,255,255,0.7);
}

.item-name {
  font-size: 15px;
  font-weight: 500;
  flex: 1;
}

.queued-by {
  font-size: 12px;
  color: rgba(255,255,255,0.4);
  flex-shrink: 0;
  padding: 0 8px;
}

.actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.action {
  padding: 6px 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: opacity 0.15s;
}

.action:hover {
  opacity: 0.85;
}

.bought {
  background: #4caf50;
  color: white;
  font-size: 15px;
}

.reject {
  background: #e53935;
  color: white;
  font-size: 15px;
}

.buy {
  background: #a0785a;
  color: white;
}

.stats {
  padding: 10px 16px;
  background: #000;
  border-top: 1px solid #222;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.defenses {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding-bottom: 8px;
  border-bottom: 1px solid #222;
  margin-bottom: 4px;
}

.defense-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 4px;
  padding: 2px 7px;
  font-size: 12px;
}

.defense-label {
  color: rgba(255,255,255,0.4);
}

.defense-value {
  color: #c8bfa8;
  font-weight: 500;
}

.stats-empty {
  font-size: 13px;
  color: #bbb;
  margin: 0;
}

.stat-group {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.group-label {
  font-size: 11px;
  color: rgba(255,255,255,0.35);
  text-transform: uppercase;
  letter-spacing: 0.04em;
  margin-bottom: 1px;
}

.stat-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.stat-row.indented {
  padding-left: 12px;
  border-left: 2px solid rgba(131,131,246,0.25);
}

.stat-text {
  font-size: 13px;
  color: #8383f6;
  flex: 1;
}
</style>
