import axios from 'axios'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
    'Accept': 'application/json;charset=UTF-8'
  }
})

const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

instance.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const config = error.config as any
    const status = error.response?.status
    const method = (config?.method || 'get').toLowerCase()
    const canRetry = config && method === 'get' && (status >= 500 || !error.response)

    if (canRetry && (config.__retryCount || 0) < 45) {
      config.__retryCount = (config.__retryCount || 0) + 1
      await wait(1000)
      return instance(config)
    }

    console.error('API error:', error)
    return Promise.reject(error)
  }
)

const api = {
  get: <T>(url: string, config?: any): Promise<T> => instance.get(url, config) as Promise<T>,
  post: <T>(url: string, data?: any, config?: any): Promise<T> => instance.post(url, data, config) as Promise<T>,
  put: <T>(url: string, data?: any, config?: any): Promise<T> => instance.put(url, data, config) as Promise<T>,
  delete: <T>(url: string, config?: any): Promise<T> => instance.delete(url, config) as Promise<T>
}

export default api
