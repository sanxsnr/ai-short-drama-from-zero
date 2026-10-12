# AI 生成水印 Android 应用
原生 Android Java 离线运行。不上传图片到服务器，不使用 AI 生成或重绘。

支持图库选择、红色半透明文字覆盖、双行组合「AI生成图像 / AI原创虚拟角色」、透明度/字号/间距调整、相册 PNG 导出。

## 手机端 GitHub 云编译
1. 打开仓库 Actions → Build AI Watermark APK。
2. 选择 feature/ai-watermark-android 分支并运行（或查看 push 自动触发）。
3. 在成功的运行页面底部 Artifacts 下载 `ai-watermark-debug-apk`。
4. 解压 zip 得到 `app-debug.apk`，在安卓手机上打开安装。

工作流为测试签名 APK，后续版本若需覆盖安装应使用固定正式签名密钥。
