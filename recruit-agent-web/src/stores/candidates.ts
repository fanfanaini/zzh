import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Candidate, ResumeDocument } from '@/types/candidate'
import { candidateApi } from '@/api/candidates'
import type { PageResponse } from '@/types/api'

export const useCandidateStore = defineStore('candidates', () => {
  const candidates = ref<Candidate[]>([])
  const currentCandidate = ref<Candidate | null>(null)
  const resumes = ref<ResumeDocument[]>([])
  const loading = ref(false)

  async function fetchCandidates() {
    loading.value = true
    try {
      const res = await candidateApi.list()
      const data = res.data as Candidate[] | PageResponse<Candidate>
      candidates.value = Array.isArray(data) ? data : data.items
    } finally {
      loading.value = false
    }
  }

  async function fetchCandidate(id: number) {
    const res = await candidateApi.get(id)
    currentCandidate.value = res.data as unknown as Candidate
    return currentCandidate.value
  }

  async function fetchResumes(candidateId: number) {
    const res = await candidateApi.listResumes(candidateId)
    resumes.value = (res.data as unknown as ResumeDocument[]) || []
  }

  return { candidates, currentCandidate, resumes, loading, fetchCandidates, fetchCandidate, fetchResumes }
})
