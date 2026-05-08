import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Descriptions, Tag, Alert, Badge, Spin, Typography, message } from 'antd';
import { getReviewById } from '../../api/reviewApi';
import type { ReviewDetail as ReviewDetailType } from '../../types';

const { Paragraph, Text } = Typography;

const riskLevelColor: Record<string, string> = {
  high: 'red',
  medium: 'orange',
  low: 'green',
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

const verifiedStatusIcon: Record<string, string> = {
  verified: '✅',
  uncertain: '⚠️',
  unverified: '❌',
};

export default function ReviewDetail() {
  const { id } = useParams<{ id: string }>();
  const [detail, setDetail] = useState<ReviewDetailType | null>(null);
  const [loading, setLoading] = useState(false);

  const fetchDetail = useCallback(async (reviewId: number) => {
    setLoading(true);
    try {
      const res = await getReviewById(reviewId);
      setDetail(res.data);
    } catch {
      message.error('获取详情失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!id) return;
    fetchDetail(Number(id));
  }, [id, fetchDetail]);

  if (loading) return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />;
  if (!detail) return null;

  return (
    <div>
      <Card title="审查摘要" style={{ marginBottom: 16 }}>
        <Descriptions column={4}>
          <Descriptions.Item label="审查结论">
            <Tag color={verdictColor[detail.verdict]}>{verdictLabel[detail.verdict] || detail.verdict}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="风险评分">{detail.riskScore ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="风险等级">
            <Tag color={riskLevelColor[detail.riskLevel]}>{detail.riskLevel}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="状态">{detail.status}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.createdAt}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{detail.updatedAt}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="原始内容" style={{ marginBottom: 16 }}>
        <Paragraph>
          <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{detail.originalContent}</pre>
        </Paragraph>
      </Card>

      <Card title="审查结果">
        {detail.violations && detail.violations.length > 0 && (
          <>
            <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 12 }}>
              违规项 ({detail.violations.length})
            </Text>
            {detail.violations.map((v, idx) => (
              <Alert
                key={idx}
                type="error"
                showIcon
                style={{ marginBottom: 12 }}
                message={
                  <div>
                    <Text strong>问题描述：</Text>{v.issueDescription}
                  </div>
                }
                description={
                  <div>
                    <Paragraph>
                      <Text strong>违规片段：</Text>
                      <Text mark>{v.segmentText}</Text>
                    </Paragraph>
                    <Paragraph>
                      <Text strong>法条引用：</Text>
                      {v.lawCitation}
                      <Badge
                        count={verifiedStatusIcon[v.verifiedStatus] || '❓'}
                        style={{ marginLeft: 8 }}
                      />
                    </Paragraph>
                    {v.verifiedLawText && (
                      <Paragraph>
                        <Text strong>法条原文：</Text>
                        <Text type="secondary">{v.verifiedLawText}</Text>
                      </Paragraph>
                    )}
                    <Paragraph>
                      <Text strong>修改建议：</Text>{v.suggestion}
                    </Paragraph>
                  </div>
                }
              />
            ))}
          </>
        )}

        {detail.missingElements && detail.missingElements.length > 0 && (
          <>
            <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 12, marginTop: 16 }}>
              缺失要素 ({detail.missingElements.length})
            </Text>
            {detail.missingElements.map((m, idx) => (
              <Alert
                key={idx}
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
                message={m.elementName}
                description={
                  <div>
                    <Paragraph><Text strong>说明：</Text>{m.description}</Paragraph>
                    <Paragraph><Text strong>要求：</Text>{m.requirement}</Paragraph>
                  </div>
                }
              />
            ))}
          </>
        )}

        {(!detail.violations || detail.violations.length === 0) &&
          (!detail.missingElements || detail.missingElements.length === 0) && (
            <Alert type="success" message="审查通过，未发现违规项或缺失要素" showIcon />
          )}
      </Card>
    </div>
  );
}
