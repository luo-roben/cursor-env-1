import { useState } from 'react';
import { Form, Select, Input, Button, Card, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { submitReview } from '../../api/reviewApi';

const { TextArea } = Input;

const contentTypeOptions = [
  { label: '营销海报', value: '营销海报' },
  { label: '微信推文', value: '微信推文' },
  { label: '招募说明书', value: '招募说明书' },
  { label: '直播话术', value: '直播话术' },
];

const productTypeOptions = [
  { label: '公募基金', value: '公募基金' },
  { label: '私募基金', value: '私募基金' },
  { label: 'ETF', value: 'ETF' },
  { label: '银行理财', value: '银行理财' },
];

const channelOptions = [
  { label: '微信朋友圈', value: '微信朋友圈' },
  { label: '官网', value: '官网' },
  { label: 'APP', value: 'APP' },
  { label: '线下活动', value: '线下活动' },
];

export default function ReviewSubmit() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (values: {
    contentType: string;
    productType: string;
    channel: string;
    originalContent: string;
  }) => {
    setLoading(true);
    try {
      const res = await submitReview({
        tenantId: 1,
        submittedBy: 1,
        ...values,
      });
      message.success('提交成功');
      navigate(`/review/${res.data.id}`);
    } catch {
      // error handled by interceptor
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card title="审查提交">
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        style={{ maxWidth: 800 }}
      >
        <Form.Item
          label="内容类型"
          name="contentType"
          rules={[{ required: true, message: '请选择内容类型' }]}
        >
          <Select options={contentTypeOptions} placeholder="请选择内容类型" />
        </Form.Item>

        <Form.Item
          label="产品类型"
          name="productType"
          rules={[{ required: true, message: '请选择产品类型' }]}
        >
          <Select options={productTypeOptions} placeholder="请选择产品类型" />
        </Form.Item>

        <Form.Item
          label="渠道"
          name="channel"
          rules={[{ required: true, message: '请选择渠道' }]}
        >
          <Select options={channelOptions} placeholder="请选择渠道" />
        </Form.Item>

        <Form.Item
          label="审查内容"
          name="originalContent"
          rules={[{ required: true, message: '请输入审查内容' }]}
        >
          <TextArea rows={10} placeholder="请粘贴需要审查的内容" />
        </Form.Item>

        <Form.Item>
          <Button type="primary" htmlType="submit" loading={loading} size="large">
            提交审查
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
