<template>
  <div class="ticket-submit">
    <n-card title="提交工单">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="标题" path="title">
          <n-input v-model:value="form.title" placeholder="请简要描述您的问题" />
        </n-form-item>
        <n-form-item label="分类" path="category">
          <n-select v-model:value="form.category" :options="categoryOptions" placeholder="请选择工单分类" />
        </n-form-item>
        <n-form-item label="优先级" path="priority">
          <n-select v-model:value="form.priority" :options="priorityOptions" placeholder="请选择优先级" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="form.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述您遇到的问题，包括操作步骤、期望结果和实际结果"
          />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" :loading="submitting" @click="handleSubmit">提交工单</n-button>
            <n-button @click="handleReset">重置</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/* eslint-disable @typescript-eslint/no-explicit-any */
import { ref, reactive } from 'vue'
import { NCard, NForm, NFormItem, NInput, NSelect, NButton, NSpace, useMessage } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import request from '@/api/request'

const message = useMessage()
const formRef = ref<FormInst | null>(null)
const submitting = ref(false)

const form = reactive({
  title: '',
  category: null as string | null,
  priority: 'medium',
  description: ''
})

const rules = {
  title: { required: true, message: '请输入标题', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change' },
  description: { required: true, message: '请输入描述', trigger: 'blur' }
}

const categoryOptions = [
  { label: 'Bug报告', value: 'bug' },
  { label: '功能建议', value: 'feature_request' },
  { label: '使用咨询', value: 'consultation' }
]

const priorityOptions = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'urgent' }
]

const emit = defineEmits<{ (e: 'submitted', ticket: any): void }>()

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitting.value = true
    const { data } = await request.post('/api/tickets', form)
    message.success('工单提交成功')
    emit('submitted', data)
    handleReset()
  } catch (e: any) {
    if (e?.message) message.error(e.message)
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  form.title = ''
  form.category = null
  form.priority = 'medium'
  form.description = ''
}
</script>
