import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Table, Tag, Card, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { getReviewList } from '../../api/reviewApi';
import type { ReviewItem } from '../../types';

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
const riskLevelColor: Record<string, string> = {
  high: 'red',
  medium: 'orange',
  low: 'green',
};

export default function ReviewList() {
  const navigate = useNavigate();
  const [data, setData] = useState<ReviewItem[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [pageNum, setPageNum] = useState(1);
  const [pageSize] = useState(20);

  const fetchData = useCallback(async (pn: number, ps: number) => {
    setLoading(true);
    try {
      const res = await getReviewList({ tenantId: 1, pageNum: pn, pageSize: ps });
      setData(res.data.list || []);
      setTotal(res.data.total || 0);
    } catch {
      message.error('获取列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(pageNum, pageSize);
  }, [pageNum, pageSize, fetchData]);

  const columns: ColumnsType<ReviewItem> = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '内容类型', dataIndex: 'contentType', width: 100 },
    { title: '产品类型', dataIndex: 'productType', width: 100 },
    {
      title: '审查结论', dataIndex: 'overallVerdict', width: 100,
      render: (v: string) => <Tag color={verdictColor[v]}>{verdictLabel[v] || v}</Tag>,
    },
    { title: '风险评分', dataIndex: 'riskScore', width: 80 },
    {
      title: '风险等级', dataIndex: 'riskLevel', width: 80,
      render: (v: string) => v ? <Tag color={riskLevelColor[v]}>{v}</Tag> : '-',
    },
    {
      title: '状态', dataIndex: 'reviewStatus', width: 80,
      render: (v: string) => <Tag>{v}</Tag>,
    },
    { title: '时间', dataIndex: 'createdAt', width: 160 },
  ];

  return (
    <Card title="审查任务列表">
      <Table<ReviewItem>
        columns={columns}
        dataSource={data}
        rowKey="id"
        loading={loading}
        pagination={{
          current: pageNum,
          pageSize,
          total,
          onChange: (p) => setPageNum(p),
        }}
        onRow={(record) => ({
          onClick: () => navigate(`/review/${record.id}`),
          style: { cursor: 'pointer' },
        })}
      />
    </Card>
  );
}
