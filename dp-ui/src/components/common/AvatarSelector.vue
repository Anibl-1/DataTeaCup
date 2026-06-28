<template>
  <div class="avatar-selector">
    <!-- 当前头像预览 -->
    <div
      class="avatar-preview"
      :style="{ width: `${size}px`, height: `${size}px` }"
      @click="panelOpen = !panelOpen"
    >
      <div
        v-if="selectedPreset"
        class="avatar-circle"
        :style="{ background: selectedPreset.gradient, width: `${size}px`, height: `${size}px`, fontSize: `${size * 0.5}px` }"
      >
        {{ selectedPreset.icon }}
      </div>
      <div
        v-else
        class="avatar-placeholder"
        :style="{ width: `${size}px`, height: `${size}px`, fontSize: `${size * 0.35}px` }"
      >
        +
      </div>
    </div>

    <!-- 选择面板 -->
    <div v-if="panelOpen" class="avatar-panel">
      <div class="avatar-grid">
        <div
          v-for="preset in avatarPresets"
          :key="preset.id"
          class="avatar-option"
          :class="{ selected: modelValue === preset.id }"
          @click="selectAvatar(preset.id)"
        >
          <div
            class="avatar-circle-sm"
            :style="{ background: preset.gradient }"
          >
            {{ preset.icon }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { avatarPresets } from '../../constants/avatarPresets'

const props = withDefaults(defineProps<{
  modelValue: string | null
  size?: number
}>(), {
  size: 64,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const panelOpen = ref(false)

const selectedPreset = computed(() => {
  return avatarPresets.find((p: { id: string }) => p.id === props.modelValue) ?? null
})

function selectAvatar(id: string) {
  emit('update:modelValue', id)
  panelOpen.value = false
}
</script>

<style scoped>
.avatar-selector {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-preview {
  cursor: pointer;
  border-radius: 50%;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.avatar-preview:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-circle {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  line-height: 1;
}

.avatar-placeholder {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e5e7eb;
  color: #9ca3af;
  font-weight: 300;
  user-select: none;
  line-height: 1;
}

.dark .avatar-placeholder {
  background: #374151;
  color: #6b7280;
}

.avatar-panel {
  width: 100%;
  max-width: 420px;
  max-height: 240px;
  overflow-y: auto;
  padding: 12px;
  background: var(--bg-primary, #fff);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.dark .avatar-panel {
  background: #1f2937;
  border-color: #374151;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.avatar-option {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 4px;
  border-radius: 12px;
  border: 3px solid transparent;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.avatar-option:hover {
  border-color: #93c5fd;
}

.avatar-option.selected {
  border-color: #3b82f6;
  transform: scale(1.05);
}

.avatar-circle-sm {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  user-select: none;
  line-height: 1;
}
</style>
