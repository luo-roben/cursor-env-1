import { useCallback, useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { getLawArticleList, createLawArticle, publishLawArticle } from '../../api/lawApi';
import type { LawArticle, LawArticleCreateRequest } from '../../types';

const { TextArea } = Input;

const statusColor: Record<string, string> = {
  draft: 'default',
  pending_review: 'orange',
  published: 'green',
  deprecated: 'red',
};

const statusLabel: Record<string, string> = {
  draft: '草稿',
  pending_review: '待审核',
  published: '已发布',
  deprecated: '已废止',
};

export default function LawArticles() {
  const [data, setData] = useState<LawArticle[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async (pn: number, ps: number) => {
    setLoading(true);
    try {
      const res = await getLawArticleList({ pageNum: pn, pageSize: ps });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch {
      message.error('获取法条列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(pageNum, pageSize);
  }, [fetchData, pageNum, pageSize]);

  const handleCreate = async (values: LawArticleCreateRequest) => {
    setSubmitting(true);
    try {
      await createLawArticle(values);
      message.success('创建成功');
      setModalOpen(false);
      form.resetFields();
      fetchData(pageNum, pageSize);
    } catch {
      // handled by interceptor
    } finally {
      setSubmitting(false);
    }
  };

  const handlePublish = async (id: number) => {
    try {
      await publishLawArticle(id);
      message.success('发布成功');
      fetchData(pageNum, pageSize);
    } catch {
      // handled by interceptor
    }
  };

  const columns: ColumnsType<LawArticle> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '法律名称', dataIndex: 'lawName', width: 200 },
    { title: '法条编号', dataIndex: 'articleNumber', width: 120 },
    { title: '规范类型', dataIndex: 'normType', width: 120 },
    { title: '效力层级', dataIndex: 'effectLevel', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (v: string) => <Tag color={statusColor[v]}>{statusLabel[v] || v}</Tag>,
    },
    {
      title: '操作',
      width: 100,
      render: (_, record) =>
        record.status === 'draft' || record.status === 'pending_review' ? (
          <Button type="link" size="small" onClick={() => handlePublish(record.id)}>
            发布
          </Button>
        ) : null,
    },
  ];

  return (
    <Card
      title="法条管理"
      extra={
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>
          新增法条
        </Button>
      }
    >
      <Table
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: pageNum,
          pageSize,
          total,
          onChange: (page, size) => {
            setPageNum(page);
            setPageSize(size);
          },
        }}
      />

      <Modal
        title="新增法条"
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item label="法律名称" name="lawName" rules={[{ required: true, message: '请输入法律名称' }]}>
            <Input placeholder="请输入法律名称" />
          </Form.Item>
          <Form.Item label="法条编号" name="articleNumber" rules={[{ required: true, message: '请输入法条编号' }]}>
            <Input placeholder="如：第二十七条" />
          </Form.Item>
          <Form.Item label="规范类型" name="normType" rules={[{ required: true, message: '请选择规范类型' }]}>
            <Select
              placeholder="请选择规范类型"
              options={[
                { label: '禁止性规范', value: '禁止性规范' },
                { label: '义务性规范', value: '义务性规范' },
                { label: '授权性规范', value: '授权性规范' },
              ]}
            />
          </Form.Item>
          <Form.Item label="效力层级" name="effectLevel" rules={[{ required: true, message: '请选择效力层级' }]}>
            <Select
              placeholder="请选择效力层级"
              options={[
                { label: '法律', value: '法律' },
                { label: '行政法规', value: '行政法规' },
                { label: '部门规章', value: '部门规章' },
                { label: '规范性文件', value: '规范性文件' },
              ]}
            />
          </Form.Item>
          <Form.Item label="法条内容" name="content" rules={[{ required: true, message: '请输入法条内容' }]}>
            <TextArea rows={4} placeholder="请输入法条原文" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} block>
              创建
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
}
