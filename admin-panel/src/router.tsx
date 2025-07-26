import { lazy, Suspense } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import { Spin } from 'antd'

const Clubs = lazy(() => import('./features/clubs/ClubsPage'))
const Reports = lazy(() => import('./features/photoReports/ReportsPage'))
const Analytics = lazy(() => import('./features/analytics/AnalyticsPage'))

const router = createBrowserRouter([
  { path: '/', element: <Clubs /> },
  { path: '/clubs', element: <Clubs /> },
  { path: '/reports', element: <Reports /> },
  { path: '/analytics', element: <Analytics /> },
])

export default function AppRouter() {
  return (
    <Suspense fallback={<Spin size="large" />}>
      <RouterProvider router={router} />
    </Suspense>
  )
}
