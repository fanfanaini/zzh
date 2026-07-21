<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
    <el-form-item label="岗位名称" prop="name">
      <el-input v-model="form.name" placeholder="例如：Java 后端开发工程师" />
    </el-form-item>

    <el-form-item label="岗位描述" prop="description">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="6"
        placeholder="填写岗位职责、任职要求、业务场景和加分项"
      />
    </el-form-item>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="必须技能" prop="requiredSkills">
          <el-select
            v-model="form.requiredSkills"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入技能后回车"
            style="width: 100%"
          >
            <el-option v-for="skill in skillSuggestions" :key="skill" :label="skill" :value="skill" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="加分技能" prop="preferredSkills">
          <el-select
            v-model="form.preferredSkills"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入技能后回车"
            style="width: 100%"
          >
            <el-option v-for="skill in skillSuggestions" :key="skill" :label="skill" :value="skill" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-form-item label="学历要求" prop="educationRequirement">
          <el-select v-model="form.educationRequirement" placeholder="选择学历" style="width: 100%">
            <el-option label="不限" value="UNKNOWN" />
            <el-option label="大专" value="COLLEGE" />
            <el-option label="本科" value="BACHELOR" />
            <el-option label="硕士" value="MASTER" />
            <el-option label="博士" value="DOCTOR" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="最低经验（年）" prop="minExperienceYears">
          <el-input-number v-model="form.minExperienceYears" :min="0" :max="30" style="width: 100%" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="最高经验（年）" prop="maxExperienceYears">
          <el-input-number v-model="form.maxExperienceYears" :min="0" :max="50" style="width: 100%" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">{{ submitLabel }}</el-button>
      <el-button @click="emit('cancel')">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { CreateJobRequest, Job } from '@/types/job'

const props = withDefaults(defineProps<{
  initialValue?: Job | null
  submitLabel?: string
}>(), {
  initialValue: null,
  submitLabel: '保存岗位',
})

const emit = defineEmits<{ submit: [data: CreateJobRequest]; cancel: [] }>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive<CreateJobRequest>({
  name: '',
  description: '',
  requiredSkills: [],
  preferredSkills: [],
  educationRequirement: 'BACHELOR',
  minExperienceYears: 1,
  maxExperienceYears: 5,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入岗位描述', trigger: 'blur' }],
}

const skillSuggestions = [
  'Java',
  'Spring Boot',
  'MySQL',
  'Redis',
  'RabbitMQ',
  'Kafka',
  'Docker',
  'Kubernetes',
  'Vue 3',
  'TypeScript',
  'Python',
  'Go',
  '微服务',
  '分布式',
]

watch(
  () => props.initialValue,
  (job) => {
    if (!job) return
    Object.assign(form, {
      name: job.name,
      description: job.description,
      requiredSkills: [...(job.requiredSkills || [])],
      preferredSkills: [...(job.preferredSkills || [])],
      educationRequirement: job.educationRequirement,
      minExperienceYears: job.minExperienceYears,
      maxExperienceYears: job.maxExperienceYears,
    })
  },
  { immediate: true },
)

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  emit('submit', {
    ...form,
    requiredSkills: [...(form.requiredSkills || [])],
    preferredSkills: [...(form.preferredSkills || [])],
  })
  submitting.value = false
}
</script>
