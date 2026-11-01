# ProotAgent

自然语言驱动的 Android Proot Linux Agent。

- Kotlin 2.0.21 + Compose（Material3）
- Gradle 8.9 / AGP 8.7.3 / JDK 17 / compileSdk 35 / arm64-v8a
- 前台服务防杀、折叠式执行进度、MCP Server/Client（后续）
- 用户自配 API（BaseURL + Key + Model）
- CI：GitHub Actions 自动签名并产出 Release APK

构建：push 到 `main` 产出 Artifact；打 tag `v*` 产出 Release。
