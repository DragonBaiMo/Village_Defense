## 调研主题
我正在进行 VillageDefense 插件的 1.8.8 单产物适配，遇到以下技术问题：CreeperAttack 模块中多处使用了 1.9+ Bukkit API，在 1.8.8 下不存在，需要可运行替代方案。

## 上下文背景
【背景】
- 当前任务：将项目固定为 **仅 1.8.8** 编译与运行（已移除 1.20.1 API 依赖，改为 1.8.8 API + v1_8_R3 NMS）。
- 为什么需要该信息：目前代码里有多个“API 不存在”的位置已临时改为 `TODO(1.8.8)`，需要可落地实现方案。
- 已知信息：
  - 1.8.8 环境下不存在以下直接 API：`Villager#setAI`、`setInvulnerable`、`setSilent`、`Creeper#setExplosionRadius`、`PotionEffectType.GLOWING`、`Entity#setRotation`、部分现代 Scoreboard 重载。
  - 当前工程已可编译通过，但功能层面存在降级点（TODO）。

## 相关伪代码

```pseudo
// TraderController
spawnTrader():
  villager = world.spawn(VILLAGER)
  villager.setCustomName(...)
  // TODO(1.8.8): setAI(false) / setInvulnerable(true/false) / setSilent(true)

// WaveController
spawnCreeper():
  creeper = world.spawn(CREEPER)
  // TODO(1.8.8): setExplosionRadius(0)

// FreezeCreeperEffect
applyFreeze(creeper):
  addPotionEffect(SLOW, duration, amplifier)
  // TODO(1.8.8): no GLOWING effect replacement

// CreeperProximityListener
tickControl(creeper):
  effect = find SLOW in creeper.getActivePotionEffects()
  if needFaceTarget:
    // TODO(1.8.8): no setRotation API

// Arena/InGameState
onTick():
  // TODO(1.8.8): advanced target manager removed; currently rely on vanilla/NMS target behavior
```

## 技术约束
【技术环境】
- 技术栈：Java 8、Spigot 1.8.8、MiniGamesBox-Classic 1.4.5
- 运行环境：Windows + Spigot 1.8.8 server
- 已有限制：
  - 只保留 1.8 单产物（不做 modern/legacy 双产物）
  - 不回退到 1.20 API 编译
  - 优先最小改动、可维护、可验证

## 待澄清问题
1. 在 Spigot 1.8.8 下，如何实现“交易村民不可移动、不可受伤、无噪音”的稳定方案？
   - 是否建议使用 NMS（如 Pathfinder 清空/NoAI NBT/拦截伤害事件）？
   - 请给出可直接落地的实现路径（推荐 1~2 种）。

2. 在 1.8.8 没有 `setExplosionRadius` 时，如何让 Creeper 保留“自爆流程”但不破坏地形、并按自定义规则结算伤害？
   - 推荐监听哪些事件（`EntityExplodeEvent` / `ExplosionPrimeEvent` / `EntityDamageByEntityEvent`）？
   - 如何避免和原版爆炸重复结算？

3. 在 1.8.8 没有 `PotionEffectType.GLOWING` 时，冻结效果的“可视提示”最佳替代是什么？
   - 粒子、盔甲架、名称颜色、标题/ActionBar 等方案对比。

4. 在 1.8.8 没有 `Entity#setRotation` 时，若需要“朝向目标”行为，推荐 NMS 更新 yaw/pitch 还是放弃该行为？
   - 若推荐 NMS，请给出低风险实现方式。

5. 当前移除了 advanced target manager（1.9+ 逻辑），在 1.8.8 下是否需要补一个简化版目标管理器？
   - 如果需要，请给出最小实现策略（村民优先 > 玩家 > 兜底）。

## 期望输出
请提供 1.8.8 可落地实现方案（优先级排序），包含：
- 每个问题的推荐方案 + 备选方案
- 关键代码示例（Java/Spigot 1.8.8 / 必要 NMS 片段）
- 风险点（兼容性、性能、维护成本）
- 建议的测试清单（如何验证不会引入副作用）
