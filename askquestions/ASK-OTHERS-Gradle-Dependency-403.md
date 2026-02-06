## 调研主题
我正在进行 Village_Defense 插件的构建与集成（新增 CreeperAttack Demo 模式），遇到以下技术问题：Gradle 无法解析依赖 `plugily.projects:MiniGamesBox-Classic:1.3.1`（PaperMC 返回 403，Plugily Maven 目前不可达/或路径不匹配），导致 `gradlew build` 失败。

## 上下文背景
【背景】这是一个 Minecraft 插件工程（Spigot/Paper，兼容 1.8.8-新版本），项目使用 Gradle（Kotlin DSL）+ shadowJar 进行依赖重定位打包。

当前构建错误现象：
- 原始构建：Gradle 优先从 `https://papermc.io/repo/repository/maven-public/` 拉取 `plugily.projects:MiniGamesBox-Classic:1.3.1`，返回 HTTP 403 Forbidden。
- 我尝试在 `build.gradle.kts` 对 PaperMC repo 做 content filter 排除 `plugily.projects`，转而从 `https://maven.plugily.xyz/releases` 获取。
- 结果变为：Gradle 报 `Could not find plugily.projects:MiniGamesBox-Classic:1.3.1`（在 `maven.plugily.xyz/releases` 和 `.../snapshots` 均找不到该坐标）。

补充验证：在当前环境用 curl 直接请求 POM：
- `https://papermc.io/repo/repository/maven-public/plugily/projects/MiniGamesBox-Classic/1.3.1/...pom` → 403（之前）/或连接异常。
- `https://maven.plugily.xyz/releases/plugily/projects/MiniGamesBox-Classic/1.3.1/...pom` → curl 结果为 000（不可达/或被代理拦截/或 TLS/证书问题）。

目标：恢复可用的依赖解析路径，使 `gradlew build` 能通过。

## 相关伪代码

```pseudo
// build.gradle.kts
repositories {
  mavenLocal()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  maven("https://oss.sonatype.org/content/repositories/snapshots")
  maven("https://oss.sonatype.org/content/repositories/central")
  maven("https://maven.plugily.xyz/releases")
  maven("https://maven.plugily.xyz/snapshots")
  // PaperMC sometimes 403 for Plugily
  maven("https://papermc.io/repo/repository/maven-public/") {
    content { excludeGroup("plugily.projects") }
  }
}

dependencies {
  implementation("plugily.projects:MiniGamesBox-Classic:1.3.1") { isTransitive = false }
  // ...
}

// gradlew build
// -> Could not resolve plugily.projects:MiniGamesBox-Classic:1.3.1
//    (either 403 or not found)
```

## 技术约束
【技术环境】
- 技术栈：Gradle 8.1.1 + Kotlin DSL（build.gradle.kts），shadowJar
- 目标：构建可打包发布（shadowJar 需要成功解析 runtimeClasspath）
- OS：Windows 11
- 限制：不使用 Docker；希望保持仓库依赖声明与原项目风格一致

## 待澄清问题
1. `plugily.projects:MiniGamesBox-Classic:1.3.1` 的正确可用 Maven 仓库地址是什么？是否有新的官方仓库域名/路径？
2. PaperMC 仓库对该 artifact 403 的原因是什么？是否需要特殊 Header/Token/UA，或该路径已废弃？
3. 如果 `maven.plugily.xyz` 无法访问，是否存在镜像仓库（例如 JitPack、repo1、GitHub Packages、或其它公共镜像）？
4. 如果只有 jar 文件可用（非 Maven），最佳实践是什么：
   - 放到 `lib/` 使用 `implementation(files("lib/...jar"))`？
   - 或发布到 `mavenLocal()`？
   - 或在 Gradle 中用 `flatDir`？
   各方案对 shadowJar relocate/minimize 的影响和注意点是什么？
5. 是否存在替代坐标/版本（例如 `MiniGamesBox-Classic` 的新版本、或 groupId/artifactId 变更），能与该项目当前代码兼容？

## 期望输出
请提供：
1) 能稳定解析 `MiniGamesBox-Classic` 的仓库方案（首选 Maven 仓库 URL + 示例 Gradle 配置）；
2) 如果需要替代坐标/版本，请给出兼容性评估要点；
3) 如果必须走本地 jar/本地仓库，请给出最稳妥的落地步骤（含 shadowJar 注意事项）。
