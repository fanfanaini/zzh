<template>
  <el-container class="app-layout">
    <Sidebar />
    <el-container direction="vertical">
      <HeaderBar />
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import Sidebar from './components/Sidebar.vue'
import HeaderBar from './components/HeaderBar.vue'
</script>

<style scoped>
.app-layout {
  height: 100vh;
  background: var(--color-bg);
}

.app-main {
  padding: 0;
  overflow-y: auto;
  background: var(--color-bg);
  min-height: calc(100vh - 56px);
}
</style>
