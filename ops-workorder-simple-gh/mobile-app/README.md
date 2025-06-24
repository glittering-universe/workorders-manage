# Android应用打包方案

## 方案一：Apache Cordova（推荐）

### 1. 环境准备

需要安装以下工具：
- Node.js (已有)
- Java JDK 8+
- Android Studio
- Android SDK

### 2. 安装Cordova

```bash
npm install -g cordova
```

### 3. 创建Cordova项目

```bash
cd ops-workorder-simple-gh
cordova create mobile-cordova com.opsworkorder.app "OpsWorkOrder"
cd mobile-cordova
```

### 4. 添加Android平台

```bash
cordova platform add android
```

### 5. 构建前端并复制到Cordova

```bash
# 在frontend-web目录下构建
cd ../frontend-web
npm run build

# 复制构建结果到Cordova的www目录
cp -r dist/* ../mobile-cordova/www/
```

### 6. 配置config.xml

需要配置网络权限、CORS等设置。

### 7. 构建Android应用

```bash
cd ../mobile-cordova
cordova build android
```

### 8. 运行和调试

```bash
cordova run android
```

## 方案二：Capacitor（现代化选择）

Capacitor是Ionic团队开发的现代化原生容器。

### 1. 安装Capacitor

```bash
cd frontend-web
npm install @capacitor/core @capacitor/cli
npm install @capacitor/android
```

### 2. 初始化Capacitor

```bash
npx cap init "OpsWorkOrder" "com.opsworkorder.app"
```

### 3. 构建并同步

```bash
npm run build
npx cap add android
npx cap sync
```

### 4. 打开Android Studio

```bash
npx cap open android
```

## 方案三：原生开发（功能最强）

如果需要更好的性能和原生功能，可以考虑：
- 使用Kotlin/Java重写Android端
- 保持后端API不变
- 使用Retrofit进行网络请求

## 配置说明

### 网络配置
由于应用需要连接后端API，需要配置：
1. 网络安全配置
2. CORS设置
3. HTTP权限

### 权限配置
可能需要的权限：
- INTERNET
- ACCESS_NETWORK_STATE
- CAMERA（如果有文件上传功能）
- WRITE_EXTERNAL_STORAGE（文件下载）

### 构建配置
- 混淆配置
- 签名配置
- 版本管理
