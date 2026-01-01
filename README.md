# scanidcard-android

移动应用开发课程实验：基于腾讯云 OCR 的身份证正反面识别 Android 应用。

## 功能
- 从相册选择图片
- 自动/指定（正面/反面）识别
- 展示识别结果页面

## 运行环境
- Android Studio
- Gradle (项目自带 wrapper)
- Android 设备或模拟器

## 配置密钥（不会上传）
1. 复制示例文件：
   - 将 `.env.example` 复制为 `.env`
2. 在 `.env` 中填写：
   - `TENCENT_SECRET_ID=...`
   - `TENCENT_SECRET_KEY=...`
   - `TENCENT_REGION=ap-guangzhou`（按需修改）

> 注意：`.env` 已被 `.gitignore` 忽略，不会提交到仓库。
