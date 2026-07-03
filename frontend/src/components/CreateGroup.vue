<template>
  <div class="container">
    <h1>Create a Group</h1>
    <form class="form" @submit.prevent>
      <div class="field">
        <label for="screenName">Screen Name</label>
        <input
          id="screenName"
          v-model="screenName"
          type="text"
          placeholder="Enter your screen name"
          autocomplete="off"
        />
      </div>
      <div class="field">
        <label for="groupName">Group Name</label>
        <input
          id="groupName"
          v-model="groupName"
          type="text"
          placeholder="Enter a group name"
          autocomplete="off"
        />
      </div>
      <div class="field">
        <label>Game Version</label>
        <div class="toggle-group">
          <button
            type="button"
            :class="['toggle-btn', { active: poeVersion === 'POE1' }]"
            @click="poeVersion = 'POE1'"
          >Path of Exile</button>
          <button
            type="button"
            :class="['toggle-btn', { active: poeVersion === 'POE2' }]"
            @click="poeVersion = 'POE2'"
          >Path of Exile 2</button>
        </div>
      </div>
      <div class="field">
        <label for="league">League</label>
        <select id="league" v-model="league" :disabled="leagueLoading">
          <option v-if="leagueLoading" value="">Loading...</option>
          <option v-for="l in leagues" :key="l.id" :value="l.id">{{ l.name }}</option>
        </select>
      </div>
      <div class="field">
        <label for="poeSessionId">POESESSID <span class="optional">(optional)</span></label>
        <input
          id="poeSessionId"
          v-model="poeSessionId"
          type="password"
          placeholder="Your PoE session ID"
          autocomplete="off"
        />
      </div>
      <p v-if="error" class="error">{{ error }}</p>
      <button type="button" :disabled="!groupName || !screenName || loading" @click="handleCreate">
        {{ loading ? 'Creating...' : 'Create' }}
      </button>
    </form>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const screenName = ref('')
const groupName = ref('')
const poeVersion = ref('POE1')
const poeSessionId = ref('')
const league = ref('')
const leagues = ref([])
const leagueLoading = ref(false)
const error = ref('')
const loading = ref(false)

async function fetchLeagues(version) {
  leagueLoading.value = true
  leagues.value = []
  try {
    const res = await fetch(`/api/data/${version}/leagues`)
    if (res.ok) {
      const data = await res.json()
      leagues.value = data.leagues
      league.value = data.defaultLeague
    }
  } catch (e) {
    leagues.value = [{ id: 'Standard', name: 'Standard' }]
    league.value = 'Standard'
  } finally {
    leagueLoading.value = false
  }
}

watch(poeVersion, (val) => { fetchLeagues(val) })
onMounted(() => { fetchLeagues(poeVersion.value) })

async function handleCreate() {
  error.value = ''
  loading.value = true
  try {
    const response = await fetch('/api/groups', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: groupName.value, screenName: screenName.value, poeVersion: poeVersion.value, poeSessionId: poeSessionId.value || null, league: league.value }),
    })
    if (!response.ok) throw new Error('Failed to create group')
    const group = await response.json()
    router.push({ path: '/group', state: { group, screenName: screenName.value } })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.container {
  max-width: 400px;
  margin: 80px auto;
  padding: 0 16px;
  font-family: sans-serif;
}

h1 {
  text-align: center;
  margin-bottom: 32px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

label {
  font-size: 14px;
  font-weight: 600;
}

.optional {
  font-weight: 400;
  color: #999;
  font-size: 12px;
}

input,
select {
  padding: 10px 12px;
  font-size: 16px;
  border: 1px solid #ccc;
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  background: #fff;
  width: 100%;
  box-sizing: border-box;
}

input:focus,
select:focus {
  border-color: #4a90e2;
}

select:disabled {
  background: #f5f5f5;
  color: #aaa;
}

.error {
  color: #d9534f;
  font-size: 14px;
  margin: 0;
}

button {
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  background-color: #4a90e2;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

button:hover:not(:disabled) {
  background-color: #357abd;
}

button:disabled {
  background-color: #a0bde0;
  cursor: not-allowed;
}

.toggle-group {
  display: flex;
  border: 1px solid #ccc;
  border-radius: 6px;
  overflow: hidden;
}

.toggle-btn {
  flex: 1;
  padding: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #555;
  background: #f5f5f5;
  border: none;
  border-radius: 0;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
}

.toggle-btn:first-child {
  border-right: 1px solid #ccc;
}

.toggle-btn.active {
  background: #4a90e2;
  color: white;
}

.toggle-btn:hover:not(.active) {
  background: #e8e8e8;
}
</style>
