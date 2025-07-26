import { useState } from 'react'
import { Table, Button, Drawer, Form, Input, Upload } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useApiQuery } from '../../api/bookingApi'

interface Club {
  id: number
  name: string
  city: string
  description: string
}

export default function ClubsPage() {
  const { data: clubs = [], isLoading } = useApiQuery<Club[]>('clubs', '/clubs')
  const [selected, setSelected] = useState<Club | null>(null)
  const [form] = Form.useForm()

  const columns: ColumnsType<Club> = [
    { title: 'ID', dataIndex: 'id' },
    { title: 'Name', dataIndex: 'name' },
    { title: 'City', dataIndex: 'city' },
    { title: 'Description', dataIndex: 'description' },
    {
      title: 'Action',
      render: (_, record) => (
        <Button type="link" onClick={() => { setSelected(record); form.setFieldsValue(record) }}>Edit</Button>
      ),
    },
  ]

  return (
    <>
      <Table rowKey="id" loading={isLoading} columns={columns} dataSource={clubs} />
      <Drawer open={!!selected} onClose={() => setSelected(null)} title="Edit club" destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name"><Input /></Form.Item>
          <Form.Item name="city" label="City"><Input /></Form.Item>
          <Form.Item name="description" label="Description"><Input.TextArea /></Form.Item>
          <Form.Item name="cover" label="Cover">
            <Upload beforeUpload={() => false} />
          </Form.Item>
          <Button type="primary">Save</Button>
        </Form>
      </Drawer>
    </>
  )
}
