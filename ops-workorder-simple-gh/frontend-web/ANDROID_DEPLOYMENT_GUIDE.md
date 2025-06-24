# Android应用部署完整指南

## 环境准备

### 1. 安装必要工具

#### Java Development Kit (JDK)
```bash
# 检查Java版本
java -version

# 如果没有安装，可以通过Homebrew安装
brew install openjdk@11
```

#### Android Studio
1. 下载并安装 [Android Studio](https://developer.android.com/studio)
2. 打开Android Studio，通过SDK Manager安装：
   - Android SDK (API Level 30+)
   - Android SDK Build-Tools
   - Android SDK Platform-Tools

#### 设置环境变量
在 `~/.zshrc` 中添加：
```bash
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools
export PATH=$PATH:$ANDROID_HOME/tools/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

然后执行：
```bash
source ~/.zshrc
```

### 2. 验证环境
```bash
# 检查Android SDK
android --version

# 检查ADB
adb version

# 检查Capacitor环境
npx cap doctor
```

## 构建流程

### 方法一：使用构建脚本（推荐）
```bash
cd frontend-web
./build-android.sh
```

### 方法二：手动构建
```bash
# 1. 构建Web应用
npm run build

# 2. 同步到Android
npx cap sync android

# 3. 打开Android Studio进行构建
npx cap open android

# 或者使用命令行构建
cd android && ./gradlew assembleDebug
```

## 开发调试

### 1. Live Reload开发
```bash
# 启动Web开发服务器
npm run dev

# 在另一个终端运行Capacitor
npx cap run android --livereload --external
```

### 2. 设备调试
```bash
# 连接设备并启用USB调试
adb devices

# 安装调试版本
cd android && ./gradlew installDebug

# 查看日志
adb logcat
```

### 3. Chrome DevTools调试
1. 在Chrome中打开 `chrome://inspect`
2. 连接设备后可以看到WebView
3. 点击"inspect"进行调试

## 配置说明

### 1. 应用配置 (capacitor.config.json)
```json
{
  "appId": "com.opsworkorder.app",
  "appName": "OpsWorkOrder",
  "webDir": "dist",
  "server": {
    "androidScheme": "https"
  },
  "plugins": {
    "CapacitorHttp": {
      "enabled": true
    }
  }
}
```

### 2. 网络配置
- 已配置允许HTTP请求（开发环境）
- 支持localhost和本地网络访问
- 生产环境建议使用HTTPS

### 3. 权限配置
应用已配置以下权限：
- INTERNET: 网络访问
- ACCESS_NETWORK_STATE: 网络状态检查
- ACCESS_WIFI_STATE: WiFi状态检查
- WRITE_EXTERNAL_STORAGE: 文件写入
- READ_EXTERNAL_STORAGE: 文件读取
- CAMERA: 相机访问（如需要）

## 生产发布

### 1. 生成签名密钥
```bash
keytool -genkey -v -keystore my-release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

### 2. 配置签名
在 `android/app/build.gradle` 中添加：
```gradle
android {
    signingConfigs {
        release {
            keyAlias 'my-key-alias'
            keyPassword 'your-key-password'
            storeFile file('my-release-key.keystore')
            storePassword 'your-store-password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
        }
    }
}
```

### 3. 构建发布版本
```bash
cd android && ./gradlew assembleRelease
```

### 4. 上传到Google Play Store
1. 创建Google Play Console账户
2. 创建新应用
3. 上传APK或AAB文件
4. 填写应用信息
5. 发布应用

## 常见问题

### 1. 网络请求失败
- 检查网络安全配置
- 确保后端API允许跨域
- 检查API地址是否正确

### 2. 构建失败
- 检查Java版本
- 清理Gradle缓存: `cd android && ./gradlew clean`
- 更新Gradle版本

### 3. 设备安装失败
- 检查设备是否开启开发者模式
- 检查USB调试是否开启
- 尝试重新连接设备

### 4. 应用崩溃
- 查看logcat日志
- 检查WebView兼容性
- 检查权限配置

## 性能优化

### 1. APK大小优化
- 启用代码混淆
- 移除未使用的资源
- 使用WebP图片格式

### 2. 运行性能优化
- 优化Vue组件渲染
- 减少HTTP请求
- 使用本地存储缓存数据

### 3. 电池优化
- 减少后台任务
- 优化网络请求频率
- 使用适当的唤醒锁策略
