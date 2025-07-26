import { ConfigProvider, Layout, Menu, notification } from 'antd'
import ruRU from 'antd/locale/ru_RU'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { QueryCache, MutationCache } from '@tanstack/react-query'
import { Link, useLocation } from 'react-router-dom'
import AppRouter from './router'

const { Sider, Content } = Layout

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) =>
      notification.error({ message: 'Error', description: (error as Error).message }),
  }),
  mutationCache: new MutationCache({
    onError: (error) =>
      notification.error({ message: 'Error', description: (error as Error).message }),
  }),
  defaultOptions: {
    queries: { retry: false },
  },
})

export default function App() {
  const location = useLocation()
  return (
    <ConfigProvider locale={ruRU}>
      <QueryClientProvider client={queryClient}>
        <Layout style={{ minHeight: '100vh' }}>
          <Sider theme="light">
            <Menu
              mode="inline"
              selectedKeys={[location.pathname]}
              items={[
                { key: '/clubs', label: <Link to="/clubs">Clubs</Link> },
                { key: '/reports', label: <Link to="/reports">Reports</Link> },
                { key: '/analytics', label: <Link to="/analytics">Analytics</Link> },
              ]}
            />
          </Sider>
          <Layout>
            <Content style={{ padding: 24 }}>
              <AppRouter />
            </Content>
          </Layout>
        </Layout>
      </QueryClientProvider>
    </ConfigProvider>
  )
}
