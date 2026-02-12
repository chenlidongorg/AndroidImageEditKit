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
