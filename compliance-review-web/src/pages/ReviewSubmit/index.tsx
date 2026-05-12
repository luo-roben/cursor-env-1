import { useState } from 'react';
import { Form, Select, Input, Button, Card, message, Upload, Row, Col, Collapse } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { submitReview, uploadAndReview } from '../../api/reviewApi';
import type { UploadFile } from 'antd/es/upload/interface';

const { TextArea } = Input;
const { Dragger } = Upload;

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

const acceptTypes = 'image/*,.pdf,.docx,.doc,.txt';

export default function ReviewSubmit() {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const navigate = useNavigate();

  const handleSubmit = async (values: {
    contentType: string;
    productType?: string;
    channel?: string;
    originalContent?: string;
  }) => {
    const file = fileList.length > 0 ? fileList[0].originFileObj : null;
    const text = values.originalContent?.trim();

    if (!file && !text) {
      message.error('请上传文件或输入审查内容');
      return;
    }

    setLoading(true);
    try {
      if (file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('contentType', values.contentType);
        if (values.productType) formData.append('productType', values.productType);
        if (values.channel) formData.append('channel', values.channel);
        formData.append('tenantId', '1');
        formData.append('submittedBy', '1');

        const res = await uploadAndReview(formData);
        message.success('提交成功');
        navigate(`/review/${res.data.id}`);
      } else {
        const res = await submitReview({
          tenantId: 1,
          submittedBy: 1,
          contentType: values.contentType,
          productType: values.productType || '',
          channel: values.channel || '',
          originalContent: text!,
        });
        message.success('提交成功');
        navigate(`/review/${res.data.id}`);
      }
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
        <Form.Item label="上传文件">
          <Dragger
            accept={acceptTypes}
            maxCount={1}
            fileList={fileList}
            beforeUpload={() => false}
            onChange={({ fileList: newFileList }) => setFileList(newFileList.slice(-1))}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽文件到此区域</p>
            <p className="ant-upload-hint">支持 图片(JPG/PNG)、PDF、Word、文本文件</p>
          </Dragger>
        </Form.Item>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label="内容类型"
              name="contentType"
              rules={[{ required: true, message: '请选择内容类型' }]}
            >
              <Select options={contentTypeOptions} placeholder="请选择内容类型" />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="产品类型" name="productType">
              <Select options={productTypeOptions} placeholder="请选择产品类型" allowClear />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item label="渠道" name="channel">
              <Select options={channelOptions} placeholder="请选择渠道" allowClear />
            </Form.Item>
          </Col>
        </Row>

        <Collapse
          ghost
          items={[
            {
              key: 'text',
              label: '或直接输入文本',
              children: (
                <Form.Item name="originalContent">
                  <TextArea rows={8} placeholder="请粘贴需要审查的内容" />
                </Form.Item>
              ),
            },
          ]}
        />

        <Form.Item style={{ marginTop: 16 }}>
          <Button type="primary" htmlType="submit" loading={loading} size="large">
            提交审查
          </Button>
        </Form.Item>
      </Form>
    </Card>
  );
}
