<template>
  <div class="page">
    <div class="header">
      <h1>{{ group.name }}</h1>
      <span class="code">{{ group.code }}</span>
      <span class="league">League: {{ group.league }}</span>
    </div>

    <div class="columns">
      <aside class="column column--members">
        <MemberList
          :members="members"
          :myScreenName="myScreenName"
          :isCreator="isCreator"
          @set-role="onSetRole"
          @transfer-creator="onTransferCreator"
        />
        <div class="session-id-section">
          <label class="session-id-label">POESESSID</label>
          <input
            v-model="poeSessionInput"
            type="password"
            placeholder="Paste session ID..."
            class="session-id-input"
            autocomplete="off"
          />
          <button class="session-id-btn" :disabled="savingSession" @click="savePoeSession">
            {{ savingSession ? 'Saving...' : 'Save' }}
          </button>
          <p v-if="sessionSaved" class="session-saved">Saved.</p>
        </div>
      </aside>

      <main class="column column--queue">
        <div class="view-toggle-group">
          <button
            type="button"
            :class="['view-toggle-btn', { active: activeTab === 'queue' }]"
            @click="activeTab = 'queue'"
          >Queue</button>
          <button
            type="button"
            :class="['view-toggle-btn', { active: activeTab === 'builds' }]"
            @click="activeTab = 'builds'"
          >Builds ({{ buildQueue.length }})</button>
        </div>

        <template v-if="activeTab === 'queue'">
          <div class="queue-header">
            <h2>Queue</h2>
          </div>
          <p v-if="queue.length === 0" class="empty">The queue is empty.</p>
          <div v-else class="queue-list">
            <div v-for="(item, index) in queue" :key="index" class="queue-row">
              <span class="position">{{ index + 1 }}</span>
              <QueueItem
                :item="item"
                :canTrade="canTrade"
                @bought="onBought(index)"
                @reject="onReject(index)"
                @buy="onBuy(index)"
              />
            </div>
          </div>
        </template>

        <template v-if="activeTab === 'builds'">
          <div class="queue-header">
            <h2>Builds</h2>
          </div>
          <p v-if="buildQueue.length === 0" class="empty">No builds imported.</p>
          <div v-else class="build-list">
            <div v-for="(build, bi) in buildQueue" :key="bi" class="build-group">
              <div class="build-group-header">
                <button
                  class="build-toggle"
                  @click="toggleBuildCollapsed(bi)"
                  :title="isBuildCollapsed(bi) ? 'Expand' : 'Collapse'"
                >{{ isBuildCollapsed(bi) ? '▶' : '▼' }}</button>
                <span class="build-name">{{ build.name }}</span>
                <span class="build-version">{{ build.poeVersion }}</span>
                <span class="build-count">{{ build.items.length }} item{{ build.items.length === 1 ? '' : 's' }}</span>
              </div>
              <div v-if="!isBuildCollapsed(bi)" class="queue-list">
                <div v-for="(item, ii) in build.items" :key="ii" class="queue-row">
                  <span class="position">{{ ii + 1 }}</span>
                  <QueueItem
                    :item="item"
                    :canTrade="canTrade"
                    @bought="onBuildItemBought(bi, ii)"
                    @reject="onBuildItemReject(bi, ii)"
                    @buy="onBuildBuy(bi, ii)"
                  />
                </div>
              </div>
            </div>
          </div>
        </template>
      </main>

      <div class="resizer" :class="{ active: resizing }" @mousedown="startResize"></div>

      <aside class="column column--create" :style="{ width: panelWidth + 'px' }">
        <CreateItemPanel
          :groupCode="group.code"
          :baseTypes="baseTypes"
          :stats="stats"
          @item-added="onItemAdded"
          @build-imported="onBuildImported"
        />
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import QueueItem from './QueueItem.vue'
import MemberList from './MemberList.vue'
import CreateItemPanel from './CreateItemPanel.vue'

const group = history.state.group
const myScreenName = history.state.screenName
const queue = ref([...group.itemQueue])
const members = ref([...group.members])
const buildQueue = ref([...(group.buildQueue || [])])

const canTrade = computed(() =>
  members.value.some(m => m.screenName === myScreenName && (m.role === 'CREATOR' || m.role === 'TRADER'))
)
const isCreator = computed(() =>
  members.value.some(m => m.screenName === myScreenName && m.role === 'CREATOR')
)
const baseTypes = ref([])
const stats = ref([])

const activeTab = ref('queue')
const buildCollapsed = ref({})

function isBuildCollapsed(bi) {
  // Default collapsed: absent/unset (undefined) counts as collapsed; only an
  // explicit false (user expanded it) shows the items.
  return buildCollapsed.value[bi] !== false
}
function toggleBuildCollapsed(bi) {
  buildCollapsed.value[bi] = buildCollapsed.value[bi] === false
}

const panelWidth = ref(750)
const resizing = ref(false)
const poeSessionInput = ref('')
const savingSession = ref(false)
const sessionSaved = ref(false)
let pollTimer = null
let mounted = false

const dingSound = new Audio('/sounds/ding.mp3')
const boomSound = new Audio('/sounds/vine-boom.mp3')
const kachingSound = new Audio('/sounds/kaching.mp3')
let lastItemCount = queue.value.length
let lastBuildCount = buildQueue.value.length
const seenNotifications = new Set()

function playDing() {
  try {
    dingSound.currentTime = 0
    dingSound.play().catch(() => {})
  } catch (e) {}
}

function playBoom() {
  try {
    boomSound.currentTime = 0
    boomSound.play().catch(() => {})
  } catch (e) {}
}

function playKaching() {
  try {
    kachingSound.currentTime = 0
    kachingSound.play().catch(() => {})
  } catch (e) {}
}

function onBeforeUnload() {
  const navType = performance.getEntriesByType('navigation')[0]?.type
  if (navType === 'reload') { return }
  navigator.sendBeacon(`/api/groups/${group.code}/leave`)
}

function startResize(e) {
  e.preventDefault()
  resizing.value = true
  const startX = e.clientX
  const startWidth = panelWidth.value

  function onMove(e) {
    const delta = startX - e.clientX
    panelWidth.value = Math.max(400, Math.min(1400, startWidth + delta))
  }

  function onUp() {
    resizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

async function syncGroup() {
  try {
    const res = await fetch(`/api/groups/${group.code}`)
    if (res.ok && mounted) {
      const live = await res.json()
      const shouldDing = canTrade.value && live.itemQueue.length > lastItemCount
      lastItemCount = live.itemQueue.length
      const shouldDingBuild = canTrade.value && (live.buildQueue || []).length > lastBuildCount
      lastBuildCount = (live.buildQueue || []).length
      queue.value = live.itemQueue
      members.value = live.members
      buildQueue.value = live.buildQueue || []
      if (shouldDing) {
        playDing()
      }
      if (shouldDingBuild) {
        playDing()
      }
      const freshNotifications = (live.myPurchaseNotifications || []).filter(n => !seenNotifications.has(n.id))
      if (freshNotifications.length > 0) {
        for (const n of freshNotifications) {
          seenNotifications.add(n.id)
          playKaching()
        }
        ackNotifications(freshNotifications.map(n => n.id))
      }
    }
  } catch (e) {
    console.warn('Poll failed', e)
  }
}

async function ackNotifications(ids) {
  try {
    await fetch(`/api/groups/${group.code}/notifications/ack`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ids }),
    })
  } catch (e) {
    console.warn('Ack notifications failed', e)
  }
}

onMounted(async () => {
  mounted = true
  try {
    const [_, btRes, stRes] = await Promise.all([
      syncGroup(),
      fetch(`/api/data/${group.poeVersion}/base-types`),
      fetch(`/api/data/${group.poeVersion}/stats`),
    ])
    if (btRes.ok) baseTypes.value = await btRes.json()
    if (stRes.ok) stats.value = await stRes.json()
  } catch (e) {
    console.warn('Could not load group data', e)
  }

  pollTimer = setInterval(syncGroup, 5000)
  window.addEventListener('beforeunload', onBeforeUnload)
})

onUnmounted(() => {
  mounted = false
  clearInterval(pollTimer)
  window.removeEventListener('beforeunload', onBeforeUnload)
})

async function removeItem(index) {
  const response = await fetch(`/api/groups/${group.code}/items/${index}`, { method: 'DELETE' })
  if (response.ok) {
    const updated = await response.json()
    queue.value = updated.itemQueue
    lastItemCount = updated.itemQueue.length
  }
}

async function onBought(index) {
  const response = await fetch(`/api/groups/${group.code}/items/${index}/bought`, { method: 'POST' })
  if (response.ok) {
    const updated = await response.json()
    queue.value = updated.itemQueue
    lastItemCount = updated.itemQueue.length
  }
}
function onReject(index) {
  playBoom()
  removeItem(index)
}
async function onBuy(index) {
  const res = await fetch(`/api/groups/${group.code}/items/${index}/buy`, { method: 'POST' })
  if (res.status === 400) {
    alert('Set your POESESSID in the sidebar before buying.')
    return
  }
  if (res.ok) {
    const { tradeUrl } = await res.json()
    window.open(tradeUrl, '_blank')
  }
}
function onItemAdded(items) {
  queue.value = items
  lastItemCount = items.length
  if (canTrade.value) {
    playDing()
  }
}

async function onBuildBuy(bi, ii) {
  const res = await fetch(`/api/groups/${group.code}/builds/${bi}/items/${ii}/buy`, { method: 'POST' })
  if (res.status === 400) {
    alert('Set your POESESSID in the sidebar before buying.')
    return
  }
  if (res.ok) {
    const { tradeUrl } = await res.json()
    window.open(tradeUrl, '_blank')
  }
}

async function onBuildItemRemove(bi, ii) {
  const res = await fetch(`/api/groups/${group.code}/builds/${bi}/items/${ii}`, { method: 'DELETE' })
  if (res.ok) {
    const updated = await res.json()
    buildQueue.value = updated.buildQueue || []
  }
}

async function onBuildItemBought(bi, ii) {
  const res = await fetch(`/api/groups/${group.code}/builds/${bi}/items/${ii}/bought`, { method: 'POST' })
  if (res.ok) {
    const updated = await res.json()
    buildQueue.value = updated.buildQueue || []
  }
}

function onBuildItemReject(bi, ii) {
  playBoom()
  onBuildItemRemove(bi, ii)
}

function onBuildImported(updatedGroup) {
  buildQueue.value = updatedGroup.buildQueue || []
  lastBuildCount = (updatedGroup.buildQueue || []).length
  playDing()
}

async function onSetRole({ screenName, role }) {
  await fetch(`/api/groups/${group.code}/members/${encodeURIComponent(screenName)}/role`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  })
  await syncGroup()
}

async function savePoeSession() {
  savingSession.value = true
  sessionSaved.value = false
  try {
    await fetch(`/api/groups/${group.code}/poe-session`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ poeSessionId: poeSessionInput.value || null }),
    })
    sessionSaved.value = true
    setTimeout(() => { sessionSaved.value = false }, 3000)
  } finally {
    savingSession.value = false
  }
}

async function onTransferCreator({ screenName }) {
  await fetch(`/api/groups/${group.code}/members/${encodeURIComponent(screenName)}/transfer-creator`, {
    method: 'POST',
  })
  await syncGroup()
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  font-family: sans-serif;
}

.header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light);
}

.header h1 {
  margin: 0;
  font-size: 22px;
}

.code {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-accent-contrast);
  background: var(--color-accent);
  padding: 3px 10px;
  border-radius: 12px;
  letter-spacing: 0.08em;
}

.league {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
}

.columns {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.column {
  overflow-y: auto;
}

.column--members {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid var(--color-border-light);
}

.column--queue {
  flex: 1;
  min-width: 0;
  padding: 20px 24px;
  border-right: none;
}

.column--create {
  flex-shrink: 0;
  border-left: none;
}

.resizer {
  width: 5px;
  flex-shrink: 0;
  background: var(--color-border-light);
  cursor: col-resize;
  transition: background 0.15s;
}

.resizer:hover,
.resizer.active {
  background: var(--color-accent);
}

h2 {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-secondary);
  margin: 0 0 12px;
}

.empty {
  color: var(--color-text-faint);
  font-size: 14px;
  margin-bottom: 16px;
}

.queue-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.queue-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.position {
  min-width: 24px;
  padding-top: 13px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-faint);
  text-align: right;
}

.view-toggle-group {
  display: flex;
  border: 1px solid #ccc;
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 16px;
  max-width: 320px;
}

.view-toggle-btn {
  flex: 1;
  padding: 8px 10px;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  background: #f5f5f5;
  border: none;
  border-radius: 0;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.view-toggle-btn:first-child {
  border-right: 1px solid #ccc;
}

.view-toggle-btn.active {
  background: #4a90e2;
  color: white;
}

.view-toggle-btn:hover:not(.active) {
  background: #e8e8e8;
}

.build-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-bottom: 16px;
}

.build-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.build-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 6px;
  border-bottom: 1px solid #e0e0e0;
}

.build-toggle {
  font-size: 10px;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px 4px;
  flex-shrink: 0;
}

.build-toggle:hover {
  color: #333;
}

.build-name {
  font-size: 14px;
  font-weight: 700;
  color: #333;
  flex: 1;
}

.build-version {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: #4a90e2;
  padding: 2px 8px;
  border-radius: 10px;
  letter-spacing: 0.04em;
}

.build-count {
  font-size: 12px;
  font-weight: 600;
  color: #999;
}

.session-id-section {
  padding: 12px 16px 16px;
  border-top: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-id-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-muted);
}

.session-id-input {
  width: 100%;
  padding: 6px 8px;
  font-size: 13px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
  background: var(--color-surface);
  color: var(--color-text);
}

.session-id-input:focus {
  border-color: var(--color-accent);
}

.session-id-btn {
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent-contrast);
  background: var(--color-accent);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  align-self: flex-start;
}

.session-id-btn:hover:not(:disabled) {
  background: var(--color-accent-hover);
}

.session-id-btn:disabled {
  background: var(--color-accent-disabled);
  cursor: not-allowed;
}

.session-saved {
  font-size: 12px;
  color: var(--color-success);
  margin: 0;
}

.optional {
  font-weight: 400;
  color: var(--color-text-optional);
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0;
}
</style>
