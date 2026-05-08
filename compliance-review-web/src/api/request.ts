import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
});

request.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    const msg = error.response?.data?.message || '请求失败，请稍后重试';
    message.error(msg);
    return Promise.reject(error);
  }
);

export default request;
