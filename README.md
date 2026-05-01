# DeathPenalty - 死亡经济惩罚插件

> **制作团队**：曙光团队  
> **制作人**：鳌江  
> **版本**：1.0.0  
> **适用服务端**：Paper / Arclight 1.16.5  
> **依赖**：Vault + 任意兼容 Vault 的经济插件（如 EssentialsX）

---

## 功能介绍

- 玩家以任何方式死亡（PvP / PvE / 虚空 / 窒息等）均触发扣款
- 支持 **百分比** 和 **固定金额** 两种扣款模式
- 余额不足时不会扣成负数
- 支持权限豁免（`deathpenalty.bypass`）
- 管理员指令支持热重载、实时修改配置、模拟测试
- 完全支持 Arclight 混合核心（Mod + 插件同服）

---

## 安装步骤

### 前置依赖

| 前置插件 | 说明 |
|--------|------|
| **Vault** | 必须 |
| **EssentialsX** / CMI / 其他经济插件 | 提供实际的经济系统 |

### 编译打包（本地 Maven）

```bash
# 进入项目目录
cd DeathPenalty

# 使用 Maven 打包
mvn clean package

# 输出文件位于
target/DeathPenalty-1.0.0.jar
```

> 需要 JDK 8 和 Maven 3.6+

### 部署

1. 将 `DeathPenalty-1.0.0.jar` 复制到服务器 `plugins/` 目录
2. 确保 `Vault.jar` 和经济插件（如 `EssentialsX.jar`）已安装
3. 启动或重启服务器
4. 配置文件自动生成于 `plugins/DeathPenalty/config.yml`

---

## 配置文件说明

```yaml
penalty:
  mode: percent        # percent=百分比，fixed=固定金额
  percent: 15.0        # 百分比模式：扣除余额的 15%
  fixed-amount: 1000.0 # 固定模式：每次扣除 1000 金币

broadcast:
  enabled: false        # 是否全服广播
  message: "..."        # 广播内容（支持 {player}/{amount} 变量）

messages:
  death-penalty: "..."  # 死亡时发给玩家的提示
  show-no-balance: true
  no-balance: "..."     # 余额为零时的提示
```

---

## 指令一览

| 指令 | 权限 | 说明 |
|------|------|------|
| `/dp reload` | `deathpenalty.admin` | 重载配置文件 |
| `/dp info` | `deathpenalty.admin` | 查看当前设置 |
| `/dp setmode <percent\|fixed>` | `deathpenalty.admin` | 切换扣款模式 |
| `/dp setpct <0-100>` | `deathpenalty.admin` | 设置百分比 |
| `/dp setfixed <金额>` | `deathpenalty.admin` | 设置固定金额 |
| `/dp test <玩家名>` | `deathpenalty.admin` | 对在线玩家模拟扣款 |

---

## 权限节点

| 权限 | 默认 | 说明 |
|------|------|------|
| `deathpenalty.admin` | OP | 使用全部管理指令 |
| `deathpenalty.bypass` | 无 | 死亡时免于金钱惩罚 |

---

## Arclight 兼容说明

- 本插件仅使用 Bukkit/Spigot API，不依赖 NMS
- 在 Arclight（Forge + Paper）环境下可正常加载
- 若服务器同时运行带死亡逻辑的 Mod，建议将事件优先级保持为 `MONITOR`（默认）以避免冲突

---

## 更新日志

### v1.0.0
- 初始发布
- 支持百分比 / 固定金额两种扣款模式
- 完整指令系统 + Tab 补全
- 余额为零保护
- 权限豁免支持
- Arclight 兼容
