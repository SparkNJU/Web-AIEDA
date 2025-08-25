<template>
  <el-dialog 
    v-model="dialogVisible" 
    title="LLM配置" 
    width="500px"
    :before-close="handleClose"
  >
    <el-form :model="form" label-width="120px" ref="formRef">
      <!-- 模式选择开关 -->
      <el-form-item label="配置模式">
        <el-switch
          v-model="isCustomMode"
          active-text="自定义模式"
          inactive-text="默认模式"
          @change="handleModeChange"
        />
      </el-form-item>

      <!-- 自定义模式下的配置项 -->
      <template v-if="isCustomMode">
        <el-form-item 
          label="API Key" 
          prop="apiKey"
          :rules="[{ required: true, message: '请输入API Key', trigger: 'blur' }]"
        >
          <el-input
            v-model="form.apiKey"
            placeholder="sk-or-v1-xxxxxxxx"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item 
          label="Base URL" 
          prop="baseUrl"
          :rules="[{ required: true, message: '请输入Base URL', trigger: 'blur' }]"
        >
          <el-input
            v-model="form.baseUrl"
            placeholder="https://openrouter.ai/api/v1"
            clearable
          />
        </el-form-item>

        <el-form-item 
          label="模型" 
          prop="model"
          :rules="[{ required: true, message: '请输入模型名称', trigger: 'blur' }]"
        >
          <el-input
            v-model="form.model"
            placeholder="google/gemini-2.5-flash"
            clearable
          />
        </el-form-item>
      </template>

      <!-- 默认模式下的提示信息 -->
      <template v-else>
        <el-form-item>
          <el-alert
            title="当前使用默认配置"
            description="系统将使用预设的LLM配置进行对话。如需自定义配置，请切换到自定义模式。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleSave"
          :loading="saving"
        >
          保存配置
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'

// 定义LLM配置接口
interface LLMConfigData {
  apiKey: string
  baseUrl: string
  model: string
}

// 定义props
const props = defineProps<{
  visible: boolean
  uid: number
  sid: number
}>()

// 定义emits
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'config-saved': [config: LLMConfigData | null]
}>()

// 响应式数据
const dialogVisible = ref(false)
const isCustomMode = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const form = reactive({
  apiKey: '',
  baseUrl: '',
  model: ''
})

// 监听visible属性变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
})

// 监听dialogVisible变化
watch(dialogVisible, (newVal) => {
  emit('update:visible', newVal)
})

// 处理模式切换
const handleModeChange = (value: boolean) => {
  if (!value) {
    // 切换到默认模式时重置表单
    resetForm()
  }
}

// 重置表单
const resetForm = () => {
  form.apiKey = ''
  form.baseUrl = ''
  form.model = ''
  formRef.value?.clearValidate()
}

// 处理关闭对话框
const handleClose = (done: () => void) => {
  if (isCustomMode.value && hasFormChanges()) {
    ElMessageBox.confirm(
      '您有未保存的配置更改，确定要关闭吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    ).then(() => {
      resetState()
      done()
    }).catch(() => {
      // 用户取消关闭
    })
  } else {
    resetState()
    done()
  }
}

// 检查表单是否有变更
const hasFormChanges = () => {
  return form.apiKey !== '' ||
         form.baseUrl !== '' ||
         form.model !== ''
}

// 重置状态
const resetState = () => {
  isCustomMode.value = false
  resetForm()
}

// 处理取消
const handleCancel = () => {
  handleClose(() => {
    dialogVisible.value = false
  })
}

// 处理保存
const handleSave = async () => {
  if (isCustomMode.value) {
    // 自定义模式需要验证表单
    if (!formRef.value) return
    
    try {
      await formRef.value.validate()
    } catch (error) {
      ElMessage.error('请填写完整的配置信息')
      return
    }
  }

  saving.value = true
  
  try {
    // 准备配置数据
    const configData: LLMConfigData | null = isCustomMode.value ? {
      apiKey: form.apiKey,
      baseUrl: form.baseUrl,
      model: form.model
    } : null

    // 发送配置保存事件
    emit('config-saved', configData)
    
    ElMessage.success('配置保存成功')
    dialogVisible.value = false
    resetState()
    
  } catch (error) {
    console.error('保存配置失败:', error)
    ElMessage.error('保存配置失败，请重试')
  } finally {
    saving.value = false
  }
}

// 暴露组件方法
defineExpose({
  resetForm,
  resetState
})
</script>

<style scoped>
.dialog-footer button:first-child {
  margin-right: 10px;
}

.el-input {
  width: 100%;
}

.el-switch {
  --el-switch-on-color: #13ce66;
  --el-switch-off-color: #8957a1;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #303133;
}

:deep(.el-alert) {
  margin: 0;
}

:deep(.el-alert__description) {
  margin: 0;
  line-height: 1.5;
}
</style>