<template>
  <div class="container">
    <h1>PoeTicketQueue</h1>
    <form class="form" @submit.prevent="handleSubmit">
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
        <label for="groupCode">Group Code</label>
        <input
          id="groupCode"
          v-model="groupCode"
          type="text"
          placeholder="Enter your group code"
          autocomplete="off"
        />
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
      <button type="submit" :disabled="loading">{{ loading ? 'Joining...' : 'Join Group' }}</button>
      <button type="button" @click="handleCreate">Create Group</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const screenName = ref('')
const groupCode = ref('')
const poeSessionId = ref('')
const error = ref('')
const loading = ref(false)

async function handleSubmit() {
  error.value = ''
  loading.value = true
  try {
    const response = await fetch(`/api/groups/${groupCode.value}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ screenName: screenName.value, poeSessionId: poeSessionId.value || null }),
    })
    if (response.status === 404) throw new Error('Group not found. Check the code and try again.')
    if (response.status === 409) throw new Error('Name already taken. Choose a different screen name.')
    if (!response.ok) throw new Error('Failed to join group')
    const group = await response.json()
    router.push({ path: '/group', state: { group, screenName: screenName.value } })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function handleCreate() {
  router.push('/create-group')
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
  color: var(--color-text-optional);
  font-size: 12px;
}

input {
  padding: 10px 12px;
  font-size: 16px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.15s;
  background: var(--color-surface);
  color: var(--color-text);
}

input:focus {
  border-color: var(--color-accent);
}

.error {
  color: var(--color-danger);
  font-size: 14px;
  margin: 0;
}

button {
  padding: 12px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-accent-contrast);
  background-color: var(--color-accent);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}

button:hover:not(:disabled) {
  background-color: var(--color-accent-hover);
}

button:disabled {
  background-color: var(--color-accent-disabled);
  cursor: not-allowed;
}

button[type="button"] {
  background-color: var(--color-surface);
  color: var(--color-accent);
  border: 2px solid var(--color-accent);
}

button[type="button"]:hover {
  background-color: var(--color-accent-soft-2);
}
</style>
