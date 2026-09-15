<script setup lang="ts">
import CertificatePage from '@/pages/CertificatePage.vue'
import BracketPage from '@/pages/BracketPage.vue'
import RoutePage from '@/pages/RoutePage.vue'
import BindingPage from '@/pages/BindingPage.vue'
import WindMatchPage from '@/pages/WindMatchPage.vue'
import ReturnPage from '@/pages/ReturnPage.vue'
import { ref } from 'vue'

const activeTab = ref('certificate')
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>热气球飞行基地地面固定支架飞行航线风力区间匹配系统</h1>
    </header>
    <nav class="app-nav">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="放飞证照台账" name="certificate">
          <CertificatePage />
        </el-tab-pane>
        <el-tab-pane label="支架归位交接" name="return">
          <ReturnPage />
        </el-tab-pane>
        <el-tab-pane label="地面固定支架管理" name="bracket">
          <BracketPage />
        </el-tab-pane>
        <el-tab-pane label="热气球航线管理" name="route">
          <RoutePage />
        </el-tab-pane>
        <el-tab-pane label="航线支架绑定" name="binding">
          <BindingPage />
        </el-tab-pane>
        <el-tab-pane label="风力区间匹配" name="match">
          <WindMatchPage />
        </el-tab-pane>
      </el-tabs>
    </nav>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.app-container {
  min-height: 100vh;
  background: #f5f5f5;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 30px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.app-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.app-nav {
  padding: 20px;
}

.el-tabs {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.el-tab-pane {
  padding: 20px 0;
}
</style>
