<template>
  <div class="ca-node">
    <div class="node-content" :class="{'root-node': node.level === 0}">
      <span class="toggle" @click="isOpen = !isOpen" v-if="node.children && node.children.length">
        {{ isOpen ? '▼' : '▶' }}
      </span>
      <span class="spacer" v-else></span>
      
      <span class="icon">{{ getIcon(node.type) }}</span>
      <span class="name">{{ node.name }}</span>
      <span class="badge" :class="getStatusClass(node.status)">{{ node.status }}</span>
      <span class="badge badge-info">{{ node.type }}</span>
    </div>

    <div class="children" v-if="isOpen && node.children && node.children.length">
      <CaTreeNode v-for="child in node.children" :key="child.id" :node="child" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  node: Object
})

const isOpen = ref(true)

const getIcon = (type) => {
  if (type === 'ISSUING_CA') return '🔐';
  if (type === 'RA') return '📝';
  if (type === 'EXTERNAL_RA') return '🌐';
  return '📄';
}

const getStatusClass = (status) => {
  if (status === 'ACTIVE') return 'badge-success';
  if (status === 'REVOKED') return 'badge-danger';
  return 'badge-secondary';
}
</script>

<style scoped>
.ca-node {
  margin-left: 20px;
}

.node-content {
  display: flex;
  align-items: center;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.node-content.root-node {
  font-weight: bold;
  background: #f8f9fa;
}

.toggle, .spacer {
  width: 20px;
  cursor: pointer;
  margin-right: 5px;
  text-align: center;
}

.icon { margin-right: 8px; }
.name { margin-right: 12px; flex: 1; }
.badge { padding: 2px 6px; border-radius: 4px; font-size: 11px; margin-left: 5px; }
.badge-success { background: #28a745; color: white; }
.badge-danger { background: #dc3545; color: white; }
.badge-secondary { background: #6c757d; color: white; }
.badge-info { background: #17a2b8; color: white; }
.children { margin-left: 10px; border-left: 1px dashed #ccc; }
</style>
