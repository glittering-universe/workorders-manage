#!/bin/bash

# Android应用构建脚本
# 使用方法: ./build-android.sh

set -e

echo "🚀 开始构建Android应用..."

# 检查是否在正确的目录
if [ ! -f "package.json" ]; then
    echo "❌ 请在frontend-web目录下运行此脚本"
    exit 1
fi

# 1. 安装依赖
echo "📦 安装依赖..."
npm install

# 2. 构建Web应用
echo "🔨 构建Web应用..."
npm run build

# 3. 同步到Android
echo "📱 同步到Android平台..."
npx cap sync android

# 4. 检查构建环境
echo "🔍 检查Android构建环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 未找到Java，请安装JDK"
    exit 1
fi

if [ ! -d "$ANDROID_HOME" ]; then
    echo "⚠️  未设置ANDROID_HOME环境变量"
    echo "请设置Android SDK路径，例如："
    echo "export ANDROID_HOME=/Users/\$USER/Library/Android/sdk"
fi

# 5. 构建Android APK
echo "🏗️  构建Android APK..."
cd android
./gradlew assembleDebug

echo "✅ 构建完成！"
echo "📍 APK文件位置: android/app/build/outputs/apk/debug/app-debug.apk"

# 6. 可选：安装到设备
read -p "是否要安装到连接的设备？(y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "📲 安装到设备..."
    ./gradlew installDebug
    echo "✅ 安装完成！"
fi

cd ..
echo "🎉 Android应用构建流程完成！"
