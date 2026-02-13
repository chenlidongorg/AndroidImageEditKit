# AndroidImageEditKit

Android 图片编辑库（裁剪场景），支持：

- 可选入口图片 `Uri`
- 无图状态提示“添加图片”
- 自由拖拽裁剪框（四角/四边/整体移动）
- 比例菜单：`a:b / 1:1 / 5:4 / 4:5 / 16:9 / 9:16`
- 旋转、水平翻转
- 精确导出（宽度/高度/9宫格对齐），可直接导出到相册或文件
- 确认回传结果 `Uri`
- 保存到系统相册

## 目录

- `imageeditkit/` Android Library 模块

## 构建

```bash
./gradlew :imageeditkit:assembleDebug
```

## 发布包（Maven Central）

本库默认 Maven 坐标：

- `groupId`: `org.endlessai.androidimageeditkit`
- `artifactId`: `imageeditkit`

发布方式：

1. 提交并 push 代码到仓库
2. 创建并 push tag（例如 `v0.1.1`）
3. GitHub Actions 会自动执行 `.github/workflows/publish.yml`，发布到 Maven Central

```bash
git tag v0.1.1
git push origin v0.1.1
```

发布后客户端依赖：

```kotlin
implementation("org.endlessai.androidimageeditkit:imageeditkit:0.1.1")
```

发布前需要在 GitHub 仓库配置这些 secrets：

- `SONATYPE_USERNAME`
- `SONATYPE_PASSWORD`
- `SIGNING_KEY`（ASCII armored 私钥文本）
- `SIGNING_PASSWORD`

首次发布前，请先在 Sonatype 完成 namespace 与 profile 配置：

1. 在 Central 绑定并验证 `org.endlessai.androidimageeditkit` 命名空间
2. 在 Central 生成并保存 User Token（即 `SONATYPE_USERNAME` / `SONATYPE_PASSWORD`）

若需要本地手动发布，可执行：

```bash
./gradlew \
  publishToSonatype \
  closeAndReleaseSonatypeStagingRepository
```

白板或其他客户端只需要：

1. `settings.gradle.kts` 使用 `mavenCentral()`
2. `build.gradle.kts` 依赖版本号更新到新发布版本

## 对外 API

### 1) 直接启动

```kotlin
val intent = ImageEditKit.createIntent(context, inputImageUri = optionalUri)
startActivityForResult(intent, 1001)
```

```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 1001) {
        val result = ImageEditKit.parseResult(resultCode, data)
        // result?.outputUri / outputWidth / outputHeight
    }
}
```

### 2) 推荐：`ActivityResultContract`

```kotlin
private val editLauncher = registerForActivityResult(ImageEditContract()) { result ->
    // result?.outputUri
}

editLauncher.launch(optionalInputUri)
```

## 交互说明

- 顶部：确认、保存、精确导出、添加图片
- 工作台：显示图片 + 绿色裁剪框 + 遮罩区
- 底部：比例菜单、添加图片、旋转、水平翻转

## 结果说明

- 点击“确认”后，会将导出图片保存到 App 缓存目录，并通过 `content://` `Uri` 回传。
- 点击“保存”后，会保存到系统相册目录 `Pictures/AndroidImageEditKit`。
- 点击“精确导出”后，可按输入尺寸和对齐方式导出，并选择导出到相册或文件。
