import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, ConfigProvider, theme } from 'antd';
import {
  FileSearchOutlined,
  FormOutlined,
  UnorderedListOutlined,
  BookOutlined,
} from '@ant-design/icons';
import zhCN from 'antd/locale/zh_CN';
import ReviewSubmit from './pages/ReviewSubmit';
import ReviewList from './pages/ReviewList';
import ReviewDetail from './pages/ReviewDetail';
import LawArticles from './pages/LawArticles';

const { Header, Sider, Content } = Layout;

const menuItems = [
  {
    key: '/review/submit',
    icon: <FormOutlined />,
    label: '审查提交',
  },
  {
    key: '/review/list',
    icon: <UnorderedListOutlined />,
    label: '审查列表',
  },
  {
    key: '/law/articles',
    icon: <BookOutlined />,
    label: '法条管理',
  },
];

function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKey = menuItems.find((item) =>
    location.pathname.startsWith(item.key)
  )?.key || '/review/submit';

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          padding: '0 24px',
          background: '#001529',
        }}
      >
        <FileSearchOutlined style={{ fontSize: 24, color: '#fff', marginRight: 12 }} />
        <span style={{ color: '#fff', fontSize: 18, fontWeight: 600 }}>
          智能合规审查系统
        </span>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[selectedKey]}
            items={menuItems}
            style={{ height: '100%', borderRight: 0 }}
            onClick={({ key }) => navigate(key)}
          />
        </Sider>
        <Content style={{ padding: 24, background: '#f5f5f5' }}>
          <Routes>
            <Route path="/review/submit" element={<ReviewSubmit />} />
            <Route path="/review/list" element={<ReviewList />} />
            <Route path="/review/:id" element={<ReviewDetail />} />
            <Route path="/law/articles" element={<LawArticles />} />
            <Route path="*" element={<Navigate to="/review/submit" replace />} />
          </Routes>
        </Content>
      </Layout>
    </Layout>
  );
}

function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        algorithm: theme.defaultAlgorithm,
      }}
    >
      <BrowserRouter>
        <AppLayout />
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default App;
