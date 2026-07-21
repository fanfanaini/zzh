import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Job } from '@/types/job'
import { jobApi } from '@/api/jobs'
import type { PageResponse } from '@/types/api'

export const useJobStore = defineStore('jobs', () => {
  const jobs = ref<Job[]>([])
  const currentJob = ref<Job | null>(null)
  const loading = ref(false)

  async function fetchJobs() {
    loading.value = true
    try {
      const res = await jobApi.list()
      const data = res.data as Job[] | PageResponse<Job>
      jobs.value = Array.isArray(data) ? data : data.items
    } finally {
      loading.value = false
    }
  }

  async function fetchJob(id: number) {
    const res = await jobApi.get(id)
    currentJob.value = res.data as unknown as Job
    return currentJob.value
  }

  return { jobs, currentJob, loading, fetchJobs, fetchJob }
})
