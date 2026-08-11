<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑通道' : '新增通道'"
    width="560px"
    top="8vh"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @open="initForm"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px" label-position="right" class="channel-form">
      <el-tabs v-model="activeTab" class="channel-tabs">
        <!-- 基础信息 -->
        <el-tab-pane label="基础信息" name="basic">
          <el-form-item label="通道编码" prop="code">
            <el-input v-model="formData.code" placeholder="如: cmpp-emay" :disabled="isEdit" />
          </el-form-item>
          <el-form-item label="通道名称" prop="name">
            <el-input v-model="formData.name" placeholder="如: 亿美软通" />
          </el-form-item>
          <!-- IP 与端口特殊：两者共占一行 -->
          <div class="host-port-row">
            <el-form-item label="服务器地址" prop="host" class="host-field">
              <el-input v-model="formData.host" placeholder="如: 127.0.0.1" />
            </el-form-item>
            <el-form-item label="端口" prop="port" label-width="56px" class="port-field">
              <el-input-number v-model="formData.port" :min="1" :max="65535" controls-position="right" style="width: 100%" />
            </el-form-item>
          </div>
          <el-form-item label="描述">
            <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="可选备注描述" />
          </el-form-item>
          <el-form-item label="成本价(元/条)">
            <el-input-number v-model="formData.costPrice" :min="0" :precision="4" :step="0.01" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-tab-pane>

        <!-- 认证配置 -->
        <el-tab-pane label="认证配置" name="auth">
          <el-form-item label="SP企业代码" prop="spId">
            <el-input v-model="formData.spId" placeholder="如: test" />
          </el-form-item>
          <el-form-item label="共享密钥" prop="sharedSecret">
            <el-input v-model="formData.sharedSecret" placeholder="请输入密钥" show-password />
          </el-form-item>
        </el-tab-pane>

        <!-- 协议与参数 -->
        <el-tab-pane label="协议与参数" name="protocol">
          <el-form-item label="CMPP版本" prop="version">
            <el-select v-model="formData.version" style="width: 100%">
              <el-option label="CMPP 2.0 (0x20)" :value="32" />
              <el-option label="CMPP 2.1 (0x21)" :value="33" />
            </el-select>
          </el-form-item>
          <el-form-item label="最大并发数" prop="maxConcurrent">
            <el-input-number v-model="formData.maxConcurrent" :min="1" :max="100" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="连接超时(ms)" prop="connectTimeout">
            <el-input-number v-model="formData.connectTimeout" :min="1000" :max="30000" :step="500" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="心跳间隔(s)" prop="heartbeatInterval">
            <el-input-number v-model="formData.heartbeatInterval" :min="10" :max="300" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="重连间隔(s)" prop="reconnectInterval">
            <el-input-number v-model="formData.reconnectInterval" :min="5" :max="120" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="最大重连(s)" prop="maxReconnectInterval">
            <el-input-number v-model="formData.maxReconnectInterval" :min="10" :max="300" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-tab-pane>
      </el-tabs>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Channel } from '../../types'
import { createChannel, updateChannel } from '../../api/channel'

const props = defineProps<{
  modelValue: boolean
  /** 传入则为编辑模式，null 为新增 */
  channel: Channel | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  /** 保存成功后触发，父组件刷新列表 */
  saved: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const activeTab = ref('basic')

const isEdit = computed(() => !!props.channel)

/** 必填字段所在标签页：校验失败时自动跳转定位 */
const fieldTabMap: Record<string, 'basic' | 'auth'> = {
  code: 'basic', name: 'basic', host: 'basic', port: 'basic',
  spId: 'auth', sharedSecret: 'auth'
}

const defaultForm = {
  code: '', name: '', host: '', port: 7890, spId: '', sharedSecret: '',
  version: 32, heartbeatInterval: 60, reconnectInterval: 10,
  maxReconnectInterval: 60, connectTimeout: 5000, maxConcurrent: 10,
  costPrice: 0.03,
  status: 1, description: ''
}
const formData = reactive({ ...defaultForm })

const formRules: FormRules = {
  code: [{ required: true, message: '请输入通道编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入通道名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入服务器地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  spId: [{ required: true, message: '请输入SP企业代码', trigger: 'blur' }],
  sharedSecret: [{ required: true, message: '请输入共享密钥', trigger: 'blur' }]
}

/** 打开时初始化表单并回到第一个标签页 */
const initForm = () => {
  activeTab.value = 'basic'
  if (props.channel) {
    const c = props.channel
    Object.assign(formData, {
      code: c.code, name: c.name, host: c.host, port: c.port,
      spId: c.spId, sharedSecret: c.sharedSecret, version: c.version,
      heartbeatInterval: c.heartbeatInterval, reconnectInterval: c.reconnectInterval,
      maxReconnectInterval: c.maxReconnectInterval, connectTimeout: c.connectTimeout,
      maxConcurrent: c.maxConcurrent, costPrice: c.costPrice ?? 0.03, status: c.status, description: c.description
    })
  } else {
    Object.assign(formData, defaultForm)
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (invalidFields: any) {
    /* 校验失败：跳转到第一个错误字段所在标签页 */
    const firstField = Object.keys(invalidFields || {})[0]
    if (firstField && fieldTabMap[firstField]) {
      activeTab.value = fieldTabMap[firstField]
    }
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && props.channel) {
      await updateChannel(props.channel.id, { ...formData })
      ElMessage.success('通道更新成功')
    } else {
      await createChannel({ ...formData })
      ElMessage.success('通道创建成功')
    }
    emit('update:modelValue', false)
    emit('saved')
  } catch { /* handled by interceptor */ }
  submitting.value = false
}
</script>

<style scoped>
/* 表单区：小屏内部滚动，避免弹窗溢出视口 */
.channel-form {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 4px;
}

/* 标签页头与内容间距 */
.channel-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.channel-tabs :deep(.el-form-item) {
  margin-bottom: 18px;
}

/* IP 与端口共占一行：地址自适应 + 端口定宽 */
.host-port-row {
  display: flex;
  gap: 12px;
}

.host-port-row .host-field {
  flex: 1;
  min-width: 0;
}

.host-port-row .port-field {
  width: 190px;
  flex-shrink: 0;
}
</style>
