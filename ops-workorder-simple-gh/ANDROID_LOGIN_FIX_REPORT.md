# Android登录问题修复报告

## 问题描述
Android Studio中的应用可以打开，但无法登录成功。

## 问题分析
经过诊断，发现问题的根本原因在于：

1. **API配置问题**：前端应用硬编码使用`http://localhost:8080/api`作为API基础URL，但在Android环境中，`localhost`指向的是Android设备本身，而不是开发机器。

2. **网络配置**：需要使用不同的IP地址来访问开发机器上的后端服务：
   - Android模拟器：需要使用`10.0.2.2`来访问宿主机的localhost
   - Android真机：需要使用开发机器在局域网中的IP地址（如：`10.197.76.173`）

## 修复方案

### 1. 修改前端API配置
- 在`src/App.vue`中添加了智能API配置选择
- 根据运行环境（Web浏览器 vs Android应用）自动选择正确的API基础URL
- 引入了`src/config/mobile.ts`配置文件来管理移动端特定的配置

### 2. 更新移动端配置
- 在`src/config/mobile.ts`中配置了多个可能的IP地址
- 添加了自动检测和连接测试功能
- 支持Android模拟器（`10.0.2.2`）和真机（`10.197.76.173`）的不同网络环境

### 3. Android网络安全配置
- 更新了`android/app/src/main/res/xml/network_security_config.xml`
- 添加了本机IP地址`10.197.76.173`到允许的HTTP域名列表
- 确保Android应用可以访问所有必要的本地开发服务器

### 4. 添加调试工具
- 创建了`debug.html`页面用于网络连接诊断
- 可以通过访问`http://localhost:8080/debug.html`或在Android应用中测试网络连接

## 修复的文件列表

1. `frontend-web/src/App.vue`
   - 添加了移动端配置导入
   - 修改了API基础URL选择逻辑

2. `frontend-web/src/config/mobile.ts`
   - 添加了智能IP地址检测
   - 配置了多环境支持

3. `frontend-web/android/app/src/main/res/xml/network_security_config.xml`
   - 添加了本机IP地址到网络安全配置

4. `frontend-web/src/debug.html`
   - 创建了网络诊断工具

## 测试步骤

### 1. 重新构建和同步
```bash
cd frontend-web
npm run build
npx cap sync
```

### 2. 在Android Studio中测试
1. 打开Android Studio
2. 构建并运行应用到模拟器或真机
3. 尝试使用以下凭据登录：
   - 用户名：`admin`
   - 密码：`newpassword123`

### 3. 网络连接诊断
如果登录仍然失败，可以：
1. 在浏览器中访问`http://localhost:8080/debug.html`进行网络测试
2. 在Android应用中查看控制台日志，确认使用的API URL
3. 检查后端服务是否正在运行：`curl http://localhost:8080/api/users`

## 常见问题和解决方案

### 问题1：模拟器无法连接
- 确保使用`10.0.2.2`而不是`localhost`
- 检查防火墙设置是否阻止了8080端口

### 问题2：真机无法连接
- 确保手机和电脑在同一个WiFi网络
- 使用实际的局域网IP地址（当前为`10.197.76.173`）
- 如果IP地址变化，需要更新`mobile.ts`配置

### 问题3：网络安全错误
- 确认`network_security_config.xml`包含了正确的IP地址
- 检查`usesCleartextTraffic="true"`已启用

## 验证修复
修复完成后，Android应用应能够：
1. 自动检测运行环境并选择正确的API URL
2. 成功连接到后端服务
3. 使用正确的凭据登录成功
4. 显示"登录成功"的通知消息

## 下一步
如果问题仍然存在，建议：
1. 检查后端服务的CORS配置
2. 验证后端API端点是否正确响应
3. 使用网络抓包工具分析网络请求
