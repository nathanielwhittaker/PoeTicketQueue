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

        <div class="queue-header builds-header">
          <h2>Builds</h2>
        </div>
        <p v-if="buildQueue.length === 0" class="empty">No builds imported.</p>
        <div v-else class="build-list">
          <div v-for="(build, bi) in buildQueue" :key="bi" class="build-group">
            <div class="build-group-header">
              <span class="build-name">{{ build.name }}</span>
              <span class="build-version">{{ build.poeVersion }}</span>
              <span class="build-count">{{ build.items.length }} item{{ build.items.length === 1 ? '' : 's' }}</span>
            </div>
            <div class="queue-list">
              <div v-for="(item, ii) in build.items" :key="ii" class="queue-row">
                <span class="position">{{ ii + 1 }}</span>
                <QueueItem
                  :item="item"
                  :canTrade="canTrade"
                  @bought="onBuildItemRemove(bi, ii)"
                  @reject="onBuildItemRemove(bi, ii)"
                  @buy="onBuildBuy(bi, ii)"
                />
              </div>
            </div>
          </div>
        </div>
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

const panelWidth = ref(750)
const resizing = ref(false)
const poeSessionInput = ref('')
const savingSession = ref(false)
const sessionSaved = ref(false)
let pollTimer = null
let mounted = false

const dingSound = new Audio('/sounds/ding.mp3')
let lastItemCount = queue.value.length

function playDing() {
  try {
    dingSound.currentTime = 0
    dingSound.play().catch(() => {})
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
      queue.value = live.itemQueue
      members.value = live.members
      buildQueue.value = live.buildQueue || []
      if (shouldDing) {
        playDing()
      }
    }
  } catch (e) {
    console.warn('Poll failed', e)
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

function onBought(index) { removeItem(index) }
function onReject(index) { removeItem(index) }
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

function onBuildImported(updatedGroup) {
  buildQueue.value = updatedGroup.buildQueue || []
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
  border-bottom: 1px solid #e0e0e0;
}

.header h1 {
  margin: 0;
  font-size: 22px;
}

.code {
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: #4a90e2;
  padding: 3px 10px;
  border-radius: 12px;
  letter-spacing: 0.08em;
}

.league {
  font-size: 13px;
  font-weight: 600;
  color: #888;
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
  border-right: 1px solid #e0e0e0;
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
  background: #e0e0e0;
  cursor: col-resize;
  transition: background 0.15s;
}

.resizer:hover,
.resizer.active {
  background: #4a90e2;
}

h2 {
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #555;
  margin: 0 0 12px;
}

.empty {
  color: #aaa;
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
  color: #aaa;
  text-align: right;
}

.builds-header {
  margin-top: 24px;
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
  border-top: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-id-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #888;
}

.session-id-input {
  width: 100%;
  padding: 6px 8px;
  font-size: 13px;
  border: 1px solid #ccc;
  border-radius: 6px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.15s;
}

.session-id-input:focus {
  border-color: #4a90e2;
}

.session-id-btn {
  padding: 6px 10px;
  font-size: 13px;
  font-weight: 600;
  color: white;
  background: #4a90e2;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  align-self: flex-start;
}

.session-id-btn:hover:not(:disabled) {
  background: #357abd;
}

.session-id-btn:disabled {
  background: #a0bde0;
  cursor: not-allowed;
}

.session-saved {
  font-size: 12px;
  color: #4caf50;
  margin: 0;
}

.optional {
  font-weight: 400;
  color: #999;
  font-size: 11px;
  text-transform: none;
  letter-spacing: 0;
}
</style>
