import { Upload, List } from 'antd'
import { useApiQuery, useApiMutation } from '../../api/bookingApi'

interface Report {
  id: number
  url: string
  clubId: number
}

export default function ReportsPage() {
  const { data: reports = [] } = useApiQuery<Report[]>('reports', '/photo-reports')
  const upload = useApiMutation<unknown>('/photo-reports/1')
  return (
    <>
      <Upload.Dragger beforeUpload={(file) => { upload.mutate(file); return false }} />
      <List
        grid={{ gutter: 16, column: 4 }}
        dataSource={reports}
        renderItem={(item) => (
          <List.Item>
            <img src={item.url} style={{ width: '100%' }} />
          </List.Item>
        )}
      />
    </>
  )
}
