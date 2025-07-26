import { Card, Col, Row, List } from 'antd'
import { Line, Column } from '@ant-design/plots'
import { useApiQuery } from '../../api/bookingApi'

export default function AnalyticsPage() {
  const { data: load = [] } = useApiQuery<{ weekday: string; value: number }[]>('load', '/analytics/load')
  const { data: tables = [] } = useApiQuery<string[]>('tables', '/analytics/top-tables')
  const { data: deposits = [] } = useApiQuery<{ date: string; value: number }[]>('deps', '/analytics/deposits')

  return (
    <Row gutter={16}>
      <Col span={8}>
        <Card title="Load by weekday">
          <Column data={load} xField="weekday" yField="value" />
        </Card>
      </Col>
      <Col span={8}>
        <Card title="Top 5 tables">
          <List dataSource={tables} renderItem={(i) => <List.Item>{i}</List.Item>} />
        </Card>
      </Col>
      <Col span={8}>
        <Card title="Deposits last 30 days">
          <Line data={deposits} xField="date" yField="value" />
        </Card>
      </Col>
    </Row>
  )
}
