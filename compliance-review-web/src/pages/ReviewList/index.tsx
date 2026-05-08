import { useCallback, useEffect, useState } from 'react';
import { Table, Tag, Card, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import type { ColumnsType } from 'antd/es/table';
import { getReviewList } from '../../api/reviewApi';
import type { ReviewItem } from '../../types';

const riskLevelColor: Record<string, string> = {
  high: 'red',
  medium: 'orange',
  low: 'green',
};

const riskLevelLabel: Record<string, string> = {
  high: '高风险',
  medium: '中风险',
  low: '低风险',
};

const verdictColor: Record<string, string> = {
  violation: 'red',
  compliant: 'green',
  needs_review: 'orange',
};

const verdictLabel: Record<string, string> = {
  violation: '违规',
  compliant: '合规',
  needs_review: '待复核',
};

export default function ReviewList() {
  const [data, setData] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const navigate = useNavigate();

  const fetchData = useCallback(async (pn: number, ps: number) => {
    setLoading(true);
    try {
      const res = await getReviewList({ tenantId: 1, pageNum: pn, pageSize: ps });
      setData(res.data.records);
      setTotal(res.data.total);
    } catch {
      message.error('获取列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(pageNum, pageSize);
  }, [fetchData, pageNum, pageSize]);

  const columns: ColumnsType<ReviewItem> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '内容类型', dataIndex: 'contentType', width: 120 },
    { title: '产品类型', dataIndex: 'productType', width: 120 },
    {
      title: '审查结论',
      dataIndex: 'verdict',
      width: 100,
      render: (v: string) => v ? <Tag color={verdictColor[v]}>{verdictLabel[v] || v}</Tag> : '-',
    },
    {
      title: '风险评分',
      dataIndex: 'riskScore',
      width: 90,
      render: (v: number) => v ?? '-',
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      width: 100,
      render: (v: string) => v ? <Tag color={riskLevelColor[v]}>{riskLevelLabel[v] || v}</Tag> : '-',
    },
    { title: '状态', dataIndex: 'status', width: 100 },
    { title: '时间', dataIndex: 'createdAt', width: 180 },
  ];

  return (
    <Card title="审查列表">
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
        onRow={(record) => ({
          onClick: () => navigate(`/review/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </Card>
  );
}
