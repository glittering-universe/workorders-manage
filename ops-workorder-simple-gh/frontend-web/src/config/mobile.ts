// 移动端API配置
// 用于Android应用的后端API配置

// 获取本机IP地址的函数
const getLocalIP = () => {
  // 本机实际IP地址：10.197.76.173
  // 根据设备类型返回不同的IP地址
  
  const isAndroidEmulator = navigator.userAgent.includes('Android') && 
                           (navigator.userAgent.includes('GooglePhone') || 
                            navigator.userAgent.includes('Emulator'));
  
  if (isAndroidEmulator) {
    return '10.0.2.2';      // Android模拟器
  } else {
    return '10.195.66.17';  // 真机访问本机IP
  }
};

export const API_CONFIG = {
  // 开发环境配置
  development: {
    baseURL: `http://${getLocalIP()}:8080/api`, // Android模拟器/真机访问本机
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

// 创建一个可以自动测试连接的API实例
export const createMobileApi = async () => {
  const possibleIPs = [
    '10.0.2.2',        // Android模拟器
    '10.195.66.17',   // 本机IP地址
    'localhost'        // 本地开发
  ];

  // 测试哪个IP可以连接
  for (const ip of possibleIPs) {
    try {
      const testUrl = `http://${ip}:8080/api/users`;
      console.log(`测试连接: ${testUrl}`);
      
      const response = await axios.get(testUrl, { timeout: 3000 });
      if (response.status === 200) {
        console.log(`连接成功: ${ip}`);
        return axios.create({
          baseURL: `http://${ip}:8080/api`,
          timeout: 10000,
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
          }
        });
      }
    } catch (error) {
      console.log(`连接失败: ${ip}`, error.message);
      continue;
    }
  }

  // 如果都失败了，返回默认配置
  console.warn('所有IP地址都无法连接，使用默认配置');
  return axios.create({
    baseURL: 'http://10.0.2.2:8080/api',
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    }
  });
};

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
