import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
});

request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 0) {
      message.error(res.msg || '请求失败');
      return Promise.reject(new Error(res.msg));
    }
    return res;
  },
  (error) => {
    const msg = error.response?.data?.msg || error.response?.data?.message || '请求失败，请稍后重试';
    message.error(msg);
    return Promise.reject(error);
  }
);

export default request;
