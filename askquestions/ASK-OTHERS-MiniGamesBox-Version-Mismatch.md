## 调研主题
我正在进行 Village Defense 插件（基于 MiniGamesBox-Classic 框架）的构建修复，遇到以下技术问题：Gradle 成功连接仓库但报 HTTP 403 Forbidden 或 TLS 握手错误，且下载到的依赖包与项目源码存在严重的 API 不兼容（接口 vs 实现类）。

## 上下文背景
【背景】这是一个 Minecraft 插件工程，依赖 `plugily.projects:MiniGamesBox-Classic:1.3.1`。
当前现状：
1. 网络环境：已配置代理，但 `gradlew build` 频繁在拉取 POM/JAR 时报 TLS 错误或 403。
2. 依赖冲突：当使用 `1.3.11` 或 `1.4.3` 时，能绕过部分网络问题进入编译阶段，但报 200+ 编译错误。
3. 核心差异：
   - 源码中使用 `plugily.projects.minigamesbox.classic.user.User`（类），但新版依赖返回的是 `IUser`（接口）。
   - 源码中使用 `plugily.projects.minigamesbox.classic.arena.PluginArena`（类），但新版依赖返回的是 `IPluginArena`（接口）。
   - 缺失 `PlugilyEvent` 基类。
   - `IKit` 接口在 1.3.1+ 版本中似乎删除了 `reStock(Player)` 方法。

需求：找到一个既能下载、又与当前“类实现”风格代码兼容的 `MiniGamesBox-Classic` 版本；或者确认该项目是否曾使用过非公开的内部版本。

## 相关伪代码

    ```pseudo
    // build.gradle.kts
    dependencies {
        // 1.3.1 -> 403 Forbidden 或 TLS 错误 // ← 不确定：是否该版本已下架或路径变更
        implementation("plugily.projects:MiniGamesBox-Classic:1.3.1")
    }

    // 编译错误示例
    // error: incompatible types: IUser cannot be converted to User
    User user = plugin.getUserManager().getUser(player); // ← 不确定：getUser() 返回值类型在哪个版本发生了断层变化
    ```

## 技术约束
【技术环境】
- 技术栈：Java 8, Gradle 8.1.1 (Kotlin DSL), Spigot 1.8.8
- 运行环境：Windows 11
- 已有限制：必须保持 1.8.8 兼容性，不建议大规模重构全项目 API。

## 待澄清问题
1. `plugily.projects:MiniGamesBox-Classic:1.3.1` 的官方 Maven 路径是否已变更？为什么 PaperMC 仓库返回 403？
2. 在 MiniGamesBox 的历史版本中，哪一个版本是最后支持“直接引用 User/PluginArena 类”而非“IUser/IPluginArena 接口”的稳定版本？
3. `1.2.0-SNAPSHOT36` 之后的第一个非快照稳定版本是哪个？它是否包含 `reStock` 方法？
4. 如果无法通过远程仓库获取兼容版本，是否有替代的 Maven 坐标（例如从 `jitpack.io` 或 `repo.plugily.xyz` 获取旧版归档）？

## 期望输出
请提供：
1) 一个可用的、与“类引用”风格代码兼容的 `MiniGamesBox-Classic` 版本号；
2) 对应的可用 Maven 仓库 URL；
3) 如果官方已彻底废弃该 API 风格，请说明从 `User` 迁移到 `IUser` 时的最小代价适配方案（例如是否有适配层或中间版本）。
