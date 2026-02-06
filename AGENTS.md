# AGENTS.md - Village Defense Plugin Development Guide

---

## 1. Project Type and Runtime Environment

**Project Type**: Minecraft Minigame Plugin (Bukkit/Spigot/Paper)

**Runtime Host**: Minecraft Server (Spigot/Paper), 兼容 1.8.8 至最新版本

**Framework Dependency**: 基于 `MiniGamesBox-Classic` 框架构建，继承其竞技场、用户、套件等基础设施

**Core Gameplay**: 玩家在波次制游戏中保护村民抵御僵尸潮

**Not Responsible For**:
- 多服务器同步（BungeeCord/Velocity 层面）
- 世界生成/地形管理
- 经济系统（依赖外部 Vault）
- 权限系统（依赖外部权限插件）
- 基础竞技场框架逻辑（由 MiniGamesBox 提供）

---

## 2. Overall Architecture

**Organization Dimension**: 分层 + 事件驱动 + 状态机

```
┌─────────────────────────────────────────────────────────────────┐
│                        Main.java (Plugin Entry)                 │
│                    extends PluginMain (MiniGamesBox)            │
└─────────────────────────────────────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   ArenaRegistry │    │   ArenaManager  │    │ ArgumentsRegistry│
│  (Arena Storage)│    │ (Lifecycle Mgmt)│    │   (Commands)     │
└────────┬────────┘    └────────┬────────┘    └─────────────────┘
         │                      │
         ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Arena.java                              │
│              (Game Container + Entity Lists + State)            │
├─────────────────────────────────────────────────────────────────┤
│  enemies[] │ villagers[] │ wolves[] │ ironGolems[] │ spawnPoints│
└─────────────────────────────────────────────────────────────────┘
         │                      │                       │
         ▼                      ▼                       ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────────────┐
│ State Handlers│    │   Managers    │    │    Event Listeners    │
│ (FSM Pattern) │    │ (Sub-systems) │    │ (Bukkit Event System) │
├───────────────┤    ├───────────────┤    ├───────────────────────┤
│ StartingState │    │ ShopManager   │    │ ArenaEvents           │
│ InGameState   │    │ EnemySpawnMgr │    │ PluginEvents          │
│ EndingState   │    │ ScoreboardMgr │    │ EntityUpgradeListener │
│ RestartingState    │ TargetManager │    │ DoorBreakListener     │
└───────────────┘    │ MapRestorer   │    └───────────────────────┘
                     └───────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    creatures/ (Entity Layer)                    │
├─────────────────────────────────────────────────────────────────┤
│  v1_8_R3/              │  v1_9_UP/                              │
│  (NMS Legacy Entities) │  (Modern API Entities)                 │
│  FastZombie, HardZombie│  CustomCreature, CustomRideableCreature│
└─────────────────────────────────────────────────────────────────┘
```

**Data Flow Direction**:
1. Player Action → Bukkit Event → ArenaEvents/PluginEvents
2. State Handler (InGameState) → EnemySpawnManager → CreatureInitializer
3. Arena Timer Tick → State Handler → ArenaManager (Wave Start/End)

---

## 3. Directory and Module Responsibilities

```
src/main/java/plugily/projects/villagedefense/
├── Main.java                    [ENTRY] Plugin 入口，初始化所有子系统
├── api/                         [API] 公共事件接口，供外部插件集成
│   └── event/                   波次事件、玩家事件、游戏事件
├── arena/                       [CORE] 核心游戏逻辑，改动需谨慎
│   ├── Arena.java               游戏容器，维护实体列表和状态
│   ├── ArenaManager.java        生命周期：波次开始/结束、游戏停止
│   ├── ArenaRegistry.java       竞技场存储和加载
│   ├── ArenaUtils.java          静态工具方法
│   ├── ArenaEvents.java         核心事件监听（死亡、伤害、拾取）
│   ├── managers/                子管理器（可独立修改）
│   │   ├── ShopManager.java     商店 GUI 和购买逻辑
│   │   ├── EnemySpawnManager.java 敌人刷新和卡怪检测
│   │   ├── ScoreboardManager.java 计分板渲染
│   │   ├── CreatureTargetManager.java 生物 AI 目标分配
│   │   └── maprestorer/         地图恢复（版本分离）
│   └── states/                  状态处理器（FSM 模式）
│       ├── StartingState.java   倒计时启动
│       ├── InGameState.java     主游戏循环（刷怪、检测胜负）
│       ├── EndingState.java     游戏结束处理
│       └── RestartingState.java 重置竞技场
├── boot/                        [INIT] 初始化器（消息、占位符、配置值）
├── commands/                    [CMD] 命令处理
│   └── arguments/               命令参数注册
├── creatures/                   [ENTITY] 生物生成和 AI
│   ├── BaseCreatureInitializer.java 接口定义
│   ├── CreatureUtils.java       版本适配工厂
│   ├── v1_8_R3/                 1.8 NMS 实现（不推荐修改）
│   └── v1_9_UP/                 1.9+ 现代实现
├── events/                      [EVENT] 其他事件监听器
├── handlers/                    [HANDLER] 功能模块
│   ├── powerup/                 Powerup 掉落和效果
│   ├── upgrade/                 实体升级系统
│   └── setup/                   竞技场设置 GUI
├── kits/                        [KIT] 玩家套件（最常扩展区域）
│   ├── free/                    免费套件
│   ├── level/                   等级解锁套件
│   └── premium/                 高级套件
└── utils/                       [UTIL] 工具类
```

**Entry Point**: `Main.java` → `onEnable()` → `initializePluginClasses()` → `addKits()`

**Modification Safety**:
| Module | Frequency | Risk |
|--------|-----------|------|
| kits/ | High | Low - 独立模块 |
| handlers/ | Medium | Low - 功能隔离 |
| arena/managers/ | Medium | Medium - 需测试 |
| arena/states/ | Low | Medium - 影响游戏流程 |
| arena/Arena.java | Low | High - 核心容器 |
| creatures/v1_8_R3/ | Rare | High - NMS 依赖 |

---

## 4. Core Flows

### 4.1 Game Lifecycle Flow

```
1. [ArenaRegistry] registerArenas() - 从 arenas.yml 加载竞技场配置
2. [Arena] constructor - 初始化状态处理器、管理器
3. [SignManager] loadSigns() - 加载加入标牌

玩家加入:
4. [ArenaManager] joinAttempt() - 验证并加入玩家
5. [Arena] addPlayer() - 添加到玩家列表

状态流转:
6. WAITING → STARTING (达到最小玩家数)
   └─ [StartingState] handleCall() - 倒计时
7. STARTING → IN_GAME (倒计时结束)
   └─ [Arena] spawnVillagers() - 生成村民
   └─ [ArenaManager] startWave() - 开始第一波
8. IN_GAME (循环)
   └─ [InGameState] handleCall() - 每 tick 检查
       ├─ 刷怪: EnemySpawnManager.spawnEnemies()
       ├─ AI 目标: CreatureTargetManager.targetCreatures()
       ├─ 胜负检测: villagers.isEmpty() → stopGame()
       └─ 波次结束: zombiesLeft <= 0 → endWave()
9. IN_GAME → ENDING (村民死光或达到最大波次)
   └─ [EndingState] handleCall() - 结算奖励
10. ENDING → RESTARTING
    └─ [RestartingState] handleCall() - 清理实体、恢复地图
11. RESTARTING → WAITING
    └─ [MapRestorerManager] clearArena()
```

### 4.2 Wave Cycle Flow

```
1. [ArenaManager] startWave(Arena)
   └─ 计算僵尸数量: (players * 0.5) * (wave^2) / 2
   └─ 设置 ZOMBIES_TO_SPAWN 选项
   └─ 触发 VillageWaveStartEvent
   └─ 补货: kit.reStock(player)

2. [InGameState] handleCall() - 每 tick
   └─ fighting == true:
       ├─ EnemySpawnManager.spawnEnemies()
       │   └─ EnemySpawnerRegistry.spawnEnemies(random, arena)
       │       └─ 根据波次选择敌人类型
       │       └─ CreatureInitializer.spawnXxxZombie()
       ├─ CreatureTargetManager.targetCreatures()
       └─ zombiesLeft <= 0 → endWave()

3. [ArenaManager] endWave(Arena)
   └─ 增加波次: wave + 1
   └─ 触发 VillageWaveEndEvent
   └─ 恢复玩家血量
   └─ 设置冷却计时器

4. 冷却结束 → fighting = true → startWave()
```

### 4.3 Entity Death Flow

```
1. [Bukkit] EntityDeathEvent 触发
2. [ArenaEvents] onDieEntity()
   ├─ Villager 死亡:
   │   └─ arena.removeVillager()
   │   └─ 雷击效果
   │   └─ 广播消息
   └─ Enemy 死亡:
       └─ arena.removeEnemy()
       └─ 增加 TOTAL_KILLED_ZOMBIES
       └─ 增加玩家 KILLS 统计
       └─ PowerupRegistry.spawnPowerup()
```

### 4.4 Player Death/Respawn Flow

```
1. [Bukkit] PlayerDeathEvent 触发
2. [ArenaEvents] onPlayerDie()
   └─ 清空掉落物
   └─ player.spigot().respawn()
   └─ 设置为观众模式
   └─ 隐藏玩家
   └─ 取消僵尸目标
   └─ 显示死亡标题

3. 波次结束时:
   └─ [ArenaUtils] bringDeathPlayersBack() (如果配置启用)
```

---

## 5. Core Concepts and Key Objects

### Arena
- **负责**: 游戏状态容器，维护实体列表（enemies/villagers/wolves/ironGolems）、出生点、波次信息
- **不负责**: 具体的游戏逻辑执行（由 State Handler 处理）
- **交互**: ArenaManager、所有 Managers、State Handlers

### ArenaManager
- **负责**: 高级游戏操作（波次开始/结束、玩家加入/离开、游戏停止）
- **不负责**: 每 tick 的游戏逻辑
- **交互**: Arena、User、RewardsHandler

### State Handlers (StartingState, InGameState, EndingState, RestartingState)
- **负责**: 各状态下的 tick 逻辑
- **不负责**: 状态切换决策（由 PluginArena 基类管理）
- **交互**: Arena、ArenaManager、各 Managers

### EnemySpawnerRegistry / EnemySpawnerRegistryLegacy
- **负责**: 根据波次选择和生成敌人类型
- **不负责**: 敌人 AI 行为
- **交互**: Arena、CreatureInitializer

### BaseCreatureInitializer
- **负责**: 生成各类实体（村民、狼、铁傀儡、各种僵尸）
- **不负责**: 实体的持续管理
- **交互**: Arena（存储实体）、CreatureUtils（工厂）

### Kit (FreeKit, LevelKit, PremiumKit)
- **负责**: 玩家装备、技能、补货逻辑
- **不负责**: 游戏状态
- **交互**: Player、KitRegistry

### ShopManager
- **负责**: 商店 GUI、购买验证、生物生成（狼/傀儡）
- **不负责**: 货币管理（使用 ORBS 统计）
- **交互**: Arena、Player、User

### MapRestorerManager
- **负责**: 游戏结束后恢复地图状态、清理实体
- **不负责**: 游戏中的实体管理
- **交互**: Arena

### CreatureTargetManager
- **负责**: 分配敌人 AI 目标（村民优先，然后玩家）
- **不负责**: 敌人生成
- **交互**: Arena（实体列表）

### User (from MiniGamesBox)
- **负责**: 玩家会话状态、统计数据、套件
- **不负责**: 游戏逻辑
- **交互**: UserManager、Arena

---

## 6. Extension and Evolution

### Recommended Extension Points

**新增套件**:
```java
// kits/level/NewKit.java
public class NewKit extends LevelKit {
  public NewKit() {
    setName(...);
    setKey("NewKit");
    getPlugin().getKitRegistry().registerKit(this);
  }
  @Override public void giveKitItems(Player player) { ... }
  @Override public void reStock(Player player) { ... }
}
// 在 Main.addKits() 中添加类引用
```

**新增敌人类型**:
1. 在 `creatures/v1_9_UP/` 添加新 Creature 类
2. 在 `BaseCreatureInitializer` 添加 spawn 方法
3. 在 `EnemySpawnerRegistry` 注册生成逻辑

**新增 API 事件**:
```java
// api/event/xxx/NewEvent.java
public class NewEvent extends PluginEvent {
  // 遵循 Bukkit 事件模式
}
// 在适当位置调用 Bukkit.getPluginManager().callEvent(new NewEvent(...))
```

**新增 Powerup**:
- 在 `handlers/powerup/` 添加新 Powerup 类
- 在 `PowerupHandler` 注册

### Available Extension Mechanisms

| Mechanism | Location | Use Case |
|-----------|----------|----------|
| Bukkit Events | api/event/ | 外部插件集成 |
| Kit Registry | kits/ | 新套件 |
| Enemy Spawner Registry | creatures/ | 新敌人类型 |
| Reward System | MiniGamesBox | 奖励触发 |
| Config Options | config.yml | 行为调整 |

### Not Recommended

- **直接修改 Arena.java 核心列表管理** - 影响所有功能
- **修改 v1_8_R3 NMS 代码** - 需要深入 NMS 知识
- **绕过 ArenaManager 直接操作状态** - 破坏状态一致性
- **在异步线程操作实体** - 线程安全问题

---

## 7. Modification Workflow

### Standard Change Procedure

1. **定位入口**: 确定功能属于哪个模块（套件/管理器/事件）
2. **阅读相关代码**: 理解调用链和数据流
3. **确认线程模型**: 实体操作必须主线程
4. **编写代码**: 遵循现有代码风格（2空格缩进，无星号导入）
5. **测试验证**: 
   - `./gradlew build` 编译通过
   - 服务器启动无错误
   - 游戏流程完整

### High-Risk Areas

| Area | Risk | Reason |
|------|------|--------|
| Arena.java | High | 核心容器，改动影响全局 |
| InGameState.java | High | 主游戏循环 |
| creatures/v1_8_R3/ | High | NMS 依赖，版本敏感 |
| ArenaManager | Medium | 生命周期管理 |
| EnemySpawnManager | Medium | 刷怪平衡 |

### Common Scenarios

| Scenario | Location |
|----------|----------|
| 新增套件 | kits/xxx/NewKit.java + Main.addKits() |
| 修改商店物品 | handlers/upgrade/ or ShopManager |
| 调整敌人强度 | creatures/ + config.yml |
| 新增游戏事件 | api/event/ + 触发点 |
| 修改计分板 | arena/managers/ScoreboardManager.java |
| 调整波次逻辑 | ArenaManager.startWave/endWave |

---

## 8. Project-Specific Constraints

### Thread Safety (Critical)
```java
// WRONG - 异步线程操作实体
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
  arena.spawnVillager(location); // 崩溃风险
});

// CORRECT - 主线程操作
Bukkit.getScheduler().runTask(plugin, () -> {
  arena.spawnVillager(location);
});
```

### Version Compatibility Pattern
```java
// 必须使用版本检查
if (ServerVersion.Version.isCurrentEqualOrLower(ServerVersion.Version.v1_8_R3)) {
  // 1.8 实现
} else {
  // 现代实现
}
```

### Entity List Management
- 实体死亡后必须从 Arena 列表移除
- 使用 Iterator 遍历时移除，避免 ConcurrentModificationException
- EnemySpawnManager.spawnGlitchCheck() 定期清理死亡实体

### MiniGamesBox Dependency
- 不要重写 PluginMain/PluginArena 的核心方法（除非必要）
- 使用框架提供的 MessageBuilder、User、StatsStorage
- 竞技场状态由框架管理，通过 addGameStateHandler 注册处理器

### Configuration Convention
- 所有可配置值从 config.yml 读取
- 使用 `getPlugin().getConfig().getXxx()` 或 `ConfigPreferences`
- 消息使用 MessageBuilder + language.yml key

---

## 9. Appendix

### Build Commands
```bash
# Windows - 设置 JDK 8
set PATH=F:\Java\Jdk8\bin;%PATH%

# 构建
./gradlew build

# 清理构建
./gradlew clean build
```

### Output
- JAR: `build/libs/VillageDefense-<version>.jar`
- 部署到服务器 plugins/ 目录

### Dependencies
- `plugily.projects:MiniGamesBox-Classic:1.3.1` (shaded)
- Spigot API 1.19.3 (compile) + 1.8.8 (compatibility)

### Configuration Files
| File | Purpose |
|------|---------|
| config.yml | 主配置 |
| arenas.yml | 竞技场定义 |
| kits.yml | 套件配置 |
| creatures.yml | 生物设置 |
| language.yml | 语言本地化 |
| entity_upgrades.yml | 实体升级配置 |
| powerups.yml | Powerup 配置 |
