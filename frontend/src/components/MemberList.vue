<template>
  <div class="member-list">
    <h2>Members</h2>
    <ul>
      <li v-for="member in members" :key="member.screenName" class="member-item">
        <div
          class="member-chip"
          :class="{ clickable: isCreator && member.screenName !== myScreenName }"
          @click.stop="onChipClick(member, $event)"
        >
          <span class="name">{{ member.screenName }}</span>
          <span v-if="member.role === 'CREATOR'" class="badge badge-creator">Creator</span>
          <span v-else-if="member.role === 'TRADER'" class="badge badge-trader">Trader</span>
        </div>
      </li>
    </ul>

    <Teleport to="body">
      <div
        v-if="activeMenu"
        class="popup"
        :style="{ top: popupY + 'px', left: popupX + 'px' }"
        @click.stop
      >
        <button
          v-if="activeMenuRole === 'MEMBER'"
          class="popup-btn"
          @click="onPromote(activeMenu)"
        >
          Promote to Trader
        </button>
        <button
          v-else-if="activeMenuRole === 'TRADER'"
          class="popup-btn"
          @click="onDemote(activeMenu)"
        >
          Demote to Member
        </button>
        <div class="popup-divider"></div>
        <button class="popup-btn popup-btn-danger" @click="onTransferCreator(activeMenu)">
          Give Creator Role
        </button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  members: { type: Array, required: true },
  myScreenName: { type: String, default: null },
  isCreator: { type: Boolean, default: false },
})
const emit = defineEmits(['set-role', 'transfer-creator'])

const activeMenu = ref(null)
const popupX = ref(0)
const popupY = ref(0)

const activeMenuRole = computed(() => {
  if (!activeMenu.value) { return null }
  return props.members.find(m => m.screenName === activeMenu.value)?.role ?? null
})

function onChipClick(member, event) {
  if (!props.isCreator || member.screenName === props.myScreenName) {
    return
  }
  if (activeMenu.value === member.screenName) {
    activeMenu.value = null
    return
  }
  const rect = event.currentTarget.getBoundingClientRect()
  popupX.value = rect.right + 8
  popupY.value = rect.top
  activeMenu.value = member.screenName
}

function closeMenu() {
  activeMenu.value = null
}

function onPromote(screenName) {
  emit('set-role', { screenName, role: 'TRADER' })
  activeMenu.value = null
}

function onDemote(screenName) {
  emit('set-role', { screenName, role: 'MEMBER' })
  activeMenu.value = null
}

function onTransferCreator(screenName) {
  emit('transfer-creator', { screenName })
  activeMenu.value = null
}

onMounted(() => { document.addEventListener('click', closeMenu) })
onUnmounted(() => { document.removeEventListener('click', closeMenu) })
</script>

<style scoped>
.member-list {
  padding: 16px;
}

h2 {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  position: relative;
}

.member-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  background: var(--color-surface-alt);
  border-radius: 6px;
  color: var(--color-text);
  user-select: none;
}

.member-chip.clickable {
  cursor: pointer;
}

.member-chip.clickable:hover {
  background: var(--color-hover-surface-2);
}

.name {
  font-size: 14px;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.badge {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  flex-shrink: 0;
}

.badge-creator {
  background: var(--color-badge-creator-bg);
  color: var(--color-badge-creator-text);
  border: 1px solid var(--color-badge-creator-border);
}

.badge-trader {
  background: var(--color-badge-trader-bg);
  color: var(--color-badge-trader-text);
  border: 1px solid var(--color-badge-trader-border);
}
</style>

<style>
.popup {
  position: fixed;
  background: var(--color-surface);
  border: 1px solid var(--color-border-strong);
  border-radius: 8px;
  box-shadow: 0 4px 16px var(--color-shadow-strong);
  min-width: 170px;
  z-index: 1000;
  padding: 6px 0;
}

.popup-btn {
  display: block;
  width: 100%;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text);
  transition: background 0.1s;
}

.popup-btn:hover {
  background: var(--color-accent-soft);
}

.popup-btn-danger {
  color: var(--color-danger-strong);
}

.popup-btn-danger:hover {
  background: var(--color-danger-soft);
}

.popup-divider {
  height: 1px;
  background: var(--color-border-faint);
  margin: 4px 0;
}
</style>
