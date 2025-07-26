import axios from 'axios'
import { useMutation, useQuery } from '@tanstack/react-query'
import type { UseQueryOptions, UseMutationOptions } from '@tanstack/react-query'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function useApiQuery<TQueryFnData, TError = unknown, TData = TQueryFnData>(
  key: string,
  url: string,
  options?: UseQueryOptions<TQueryFnData, TError, TData>
) {
  return useQuery<TQueryFnData, TError, TData>({
    queryKey: [key],
    queryFn: async () => (await api.get<TQueryFnData>(url)).data,
    ...options,
  })
}

export function useApiMutation<T = unknown, V = any>(url: string, options?: UseMutationOptions<T, unknown, V>) {
  return useMutation<T, unknown, V>({
    mutationFn: (data: V) => api.post<T>(url, data).then((r) => r.data),
    ...options,
  })
}

export default api
