# Common Package PRD (Android)

## 目标

- 包仓库与业务 App 仓库完全独立维护
- 包统一发布到 Maven Central
- 各客户端只通过远端版本号引用，不使用本地路径耦合

## 命名规范

- `groupId`: `org.endlessai.androidimageeditkit`
- `artifactId`: `imageeditkit`
- 代码包名：`org.endlessai.androidimageeditkit`

## 版本发布策略

1. 仅在包仓库开发与测试
2. 合并到 `main`
3. 更新 `VERSION_NAME`
4. 打 tag：`v<version>`（如 `v0.1.1`）
5. push tag 触发发布工作流

## Maven Central 发布流水

1. CI 执行 `publishToSonatype`
2. CI 执行 `closeAndReleaseSonatypeStagingRepository`
3. 等待 Central 同步完成
4. 校验坐标可下载：`org.endlessai.androidimageeditkit:imageeditkit:<version>`

## 仓库 Secrets

- `SONATYPE_USERNAME`
- `SONATYPE_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

## 客户端接入规范

1. 仅保留 `google()` 与 `mavenCentral()`
2. 依赖写法：`implementation("org.endlessai.androidimageeditkit:imageeditkit:<version>")`
3. 禁止 `includeBuild`、submodule、本地路径替换

## Whiteboard 升级流程

1. 包仓库发布新版本 `vX.Y.Z`
2. Whiteboard 将依赖版本升级到 `X.Y.Z`
3. Whiteboard 进行编译与回归测试
4. Whiteboard 独立发布
