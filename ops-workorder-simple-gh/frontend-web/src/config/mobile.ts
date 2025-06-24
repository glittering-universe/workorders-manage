// 移动端API配置
// 用于Android应用的后端API配置

export const API_CONFIG = {
  // 开发环境配置
  development: {
    baseURL: 'http://10.0.2.2:8080/api', // Android模拟器访问本机
    timeout: 10000,
  },
  
  // 生产环境配置
  production: {
    baseURL: 'https://your-domain.com/api', // 替换为实际域名
    timeout: 10000,
  },
  
  // 获取当前环境配置
  get current() {
    // 在Capacitor环境中检测
    const isCapacitor = (window as any).Capacitor?.isNativePlatform();
    const isDev = location.hostname === 'localhost' || location.hostname === '127.0.0.1';
    
    if (isCapacitor && !isDev) {
      return this.production;
    }
    return this.development;
  }
};

// 移动端特定的网络配置
export const MOBILE_CONFIG = {
  // 网络重试配置
  retry: {
    attempts: 3,
    delay: 1000,
  },
  
  // 缓存配置
  cache: {
    enabled: true,
    duration: 5 * 60 * 1000, // 5分钟
  },
  
  // 离线支持
  offline: {
    enabled: true,
    queueRequests: true,
  }
};

// 移动端优化的axios实例
import axios from 'axios';

export const mobileApi = axios.create({
  ...API_CONFIG.current,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  }
});

// 请求拦截器
mobileApi.interceptors.request.use(
  (config) => {
    // 添加token等认证信息
    const token = localStorage.getItem('token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
mobileApi.interceptors.response.use(
  (response) => response,
  (error) => {
    // 处理网络错误
    if (!error.response) {
      console.log('网络错误，请检查网络连接');
    }
    return Promise.reject(error);
  }
);
