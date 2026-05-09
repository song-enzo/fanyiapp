# 悬浮 OCR 翻译

这是一个原生 Android 项目，按需求实现全局侧边悬浮条触发、屏幕捕获、ML Kit 本地 OCR、Gemini 翻译、全屏结果展示、设置和历史记录。

## 当前功能

- 悬浮窗权限入口
- MediaProjection 屏幕捕获授权入口
- 全局侧边悬浮条前台服务
- 点击悬浮条后截屏、OCR、调用 Gemini 翻译
- 翻译结果全屏展示，支持复制、清空、重新翻译
- Gemini API Key、接口地址、模型、悬浮条位置、透明度、字体大小设置
- 本地历史记录，最多保留 100 条
- GitHub Actions 自动构建 APK

## 本地构建

本机需要安装 Android Studio 或 JDK 17 + Android SDK + Gradle。

```bash
gradle assembleDebug
```

生成的 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub 在线打包

把项目推送到 GitHub 后，进入 Actions，运行 `Build APK` 工作流。构建完成后，在 Artifacts 中下载 `OcrTranslator-APK`。

如需 Release 签名包，请在 GitHub Secrets 中配置：

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

## 使用流程

1. 安装 APK
2. 打开 APP，开启悬浮窗权限
3. 授权屏幕捕获
4. 进入设置，填写 Gemini API Key
5. 点击“启动悬浮翻译”
6. 在任意应用中点击侧边悬浮条开始 OCR 翻译

## 说明

Android 系统限制决定了首次使用或服务重启后必须出现一次屏幕捕获授权弹窗。授权成功后，本次进程内可以由悬浮条触发截屏识别。
