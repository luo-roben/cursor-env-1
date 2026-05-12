import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Card, Descriptions, Tag, Alert, Spin, Typography, message } from 'antd';
import { getReviewById } from '../../api/reviewApi';
import type { ReviewTaskResp } from '../../types';

const { Paragraph, Text } = Typography;

const riskLevelColor: Record<string, string> = { high: 'red', medium: 'orange', low: 'green' };
const verdictColor: Record<string, string> = { violation: 'red', compliant: 'green', needs_review: 'orange' };
const verdictLabel: Record<string, string> = { violation: '违规', compliant: '合规', needs_review: '待复核' };
const citationIcon: Record<string, string> = { verified: '✅', corrected: '⚠️', unverified: '❌', pending: '❓' };

export default function ReviewDetail() {
  const { id } = useParams<{ id: string }>();
  const [detail, setDetail] = useState<ReviewTaskResp | null>(null);
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
      {detail.fileUrl && (
        <Card title="上传文件" style={{ marginBottom: 16 }}>
          {detail.fileUrl.match(/\.(jpg|jpeg|png|gif|webp|bmp)$/i) ? (
            <img src={`/api/v1/files/${detail.fileUrl}`} alt="上传文件" style={{ maxWidth: '100%', maxHeight: 400 }} />
          ) : (
            <a href={`/api/v1/files/${detail.fileUrl}`} target="_blank" rel="noopener noreferrer">
              {detail.fileName || '下载查看文件'}
            </a>
          )}
        </Card>
      )}

      <Card title="审查摘要" style={{ marginBottom: 16 }}>
        <Descriptions column={4}>
          <Descriptions.Item label="审查结论">
            <Tag color={verdictColor[detail.overallVerdict]}>{verdictLabel[detail.overallVerdict] || detail.overallVerdict}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="风险评分">{detail.riskScore ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="风险等级">
            <Tag color={riskLevelColor[detail.riskLevel]}>{detail.riskLevel}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="状态">{detail.reviewStatus}</Descriptions.Item>
          <Descriptions.Item label="模型">{detail.llmModel}</Descriptions.Item>
          <Descriptions.Item label="耗时">{detail.totalLatencyMs}ms</Descriptions.Item>
          <Descriptions.Item label="创建时间">{detail.createdAt}</Descriptions.Item>
          <Descriptions.Item label="完成时间">{detail.completedAt}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="原始内容" style={{ marginBottom: 16 }}>
        <Paragraph>
          <pre style={{ whiteSpace: 'pre-wrap', margin: 0 }}>{detail.originalContent}</pre>
        </Paragraph>
      </Card>

      <Card title="审查结果">
        {detail.results && detail.results.length > 0 && (
          <>
            <Text strong style={{ fontSize: 16, display: 'block', marginBottom: 12 }}>
              违规项 ({detail.results.filter(r => r.verdict === 'violation').length})
            </Text>
            {detail.results.filter(r => r.verdict === 'violation').map((r) => (
              <Alert
                key={r.id}
                type="error"
                showIcon
                style={{ marginBottom: 12 }}
                message={
                  <div>
                    <Text strong>问题：</Text>{r.description}
                    <Tag color="red" style={{ marginLeft: 8 }}>{r.severity}</Tag>
                  </div>
                }
                description={
                  <div>
                    <Paragraph>
                      <Text strong>违规片段：</Text>
                      <Text mark>{r.originalText}</Text>
                    </Paragraph>
                    <Paragraph>
                      <Text strong>法条引用：</Text>
                      {r.citedLawName} {r.citedArticleCode}
                      <span style={{ marginLeft: 8 }}>{citationIcon[r.citationStatus] || '❓'}</span>
                    </Paragraph>
                    {r.verifiedOriginalText && (
                      <Paragraph>
                        <Text strong>法条原文（知识库回填）：</Text>
                        <Text type="secondary">{r.verifiedOriginalText}</Text>
                      </Paragraph>
                    )}
                    <Paragraph>
                      <Text strong>修改建议：</Text>{r.suggestion}
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
            {detail.missingElements.map((m) => (
              <Alert
                key={m.id}
                type="warning"
                showIcon
                style={{ marginBottom: 12 }}
                message={m.element}
                description={
                  <div>
                    <Paragraph><Text strong>要求：</Text>{m.requirement}</Paragraph>
                    <Paragraph><Text strong>建议：</Text>{m.suggestion}</Paragraph>
                    <Tag color="orange">{m.severity}</Tag>
                  </div>
                }
              />
            ))}
          </>
        )}

        {(!detail.results || detail.results.filter(r => r.verdict === 'violation').length === 0) &&
          (!detail.missingElements || detail.missingElements.length === 0) && (
            <Alert type="success" message="审查通过，未发现违规项或缺失要素" showIcon />
          )}
      </Card>
    </div>
  );
}
