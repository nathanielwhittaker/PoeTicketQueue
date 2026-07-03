import { createRouter, createWebHistory } from 'vue-router'
import JoinGroup from '../components/JoinGroup.vue'
import CreateGroup from '../components/CreateGroup.vue'
import GroupView from '../components/GroupView.vue'

const routes = [
  { path: '/join-group', component: JoinGroup },
  { path: '/create-group', component: CreateGroup },
  { path: '/group', component: GroupView },
  { path: '/', redirect: '/join-group' },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
