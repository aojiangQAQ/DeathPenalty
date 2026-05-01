# DeathPenalty - 死亡经济惩罚插件

![Minecraft](https://img.shields.io/badge/Minecraft-1.16.5-green)
![Paper/Arclight](https://img.shields.io/badge/Paper%2FArclight-Compatible-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

> **DeathPenalty**（死亡惩罚）是一个为 Minecraft 服务器设计的经济惩罚插件。玩家死亡时按百分比或固定金额扣除金币，支持 Vault 经济插件，完美兼容 Arclight 混合核心。

---

## ✨ 功能亮点

- **灵活扣款模式**  
  支持 **百分比** 和 **固定金额** 两种扣款模式，可在配置文件中自由切换

- **智能余额保护**  
  余额不足时不会扣成负数，确保玩家账户安全

- **权限豁免**  
  拥有 `deathpenalty.bypass` 权限的玩家可免于死亡扣款

- **完整管理指令**  
  支持热重载、实时修改配置、模拟测试扣款效果

- **Arclight 完美兼容**  
  纯 Bukkit/Spigot API 实现，不依赖 NMS，在 Forge + Paper 混合核心下稳定运行

- **事件优先级可调**  
  若服务器同时运行带死亡逻辑的 Mod，建议将事件优先级保持为 `MONITOR` 以避免冲突

---

## 📂 目录结构

```
DeathPenalty/
├── pom.xml
├── README.md
├── LICENSE
├── .gitignore
└── src/
    └── main/
        ├── java/
        │   └── com/shuguangteam/deathpenalty/...
        └── resources/
            ├── plugin.yml
            └── config.yml
```

---

## 🛠 本地构建

确保使用 **JDK 8+** 和 **Maven 3.6+**：

```bash
# 克隆仓库
git clone https://github.com/aojiangQAQ/DeathPenalty.git
cd DeathPenalty

# Maven 打包
mvn clean package

# 生成 target/DeathPenalty-1.0.0.jar
```

---

## 🚀 安装与配置

### 前置依赖

| 前置插件 | 说明 |
|--------|------|
| **Vault** | 必须 |
| **EssentialsX** / CMI / 其他经济插件 | 提供实际的经济系统 |

### 安装步骤

1. 确保服务器已安装 **Vault** 及经济插件（如 EssentialsX）
2. 将 `DeathPenalty-1.0.0.jar` 复制到服务器 `plugins/` 目录
3. 启动或重启服务器，`plugins/DeathPenalty/config.yml` 自动生成
4. 根据需要修改配置，执行 `/dp reload` 生效

---

## 📝 指令 & 权限

| 指令 | 权限 | 说明 |
|------|------|------|
| `/dp reload` | `deathpenalty.admin` | 重载配置文件 |
| `/dp info` | `deathpenalty.admin` | 查看当前设置 |
| `/dp setmode <percent\|fixed>` | `deathpenalty.admin` | 切换扣款模式 |
| `/dp setpct <0-100>` | `deathpenalty.admin` | 设置百分比 |
| `/dp setfixed <金额>` | `deathpenalty.admin` | 设置固定金额 |
| `/dp test <玩家名>` | `deathpenalty.admin` | 对在线玩家模拟扣款 |

### 权限节点

| 权限 | 默认 | 说明 |
|------|------|------|
| `deathpenalty.admin` | OP | 使用全部管理指令 |
| `deathpenalty.bypass` | 无 | 死亡时免于金钱惩罚 |

---

## 🔧 配置说明

```yaml
penalty:
  mode: percent        # percent=百分比，fixed=固定金额
  percent: 15.0        # 百分比模式：扣除余额的 15%
  fixed-amount: 1000.0 # 固定模式：每次扣除 1000 金币

broadcast:
  enabled: false        # 是否全服广播
  message: "&6{player} 死亡扣款 {amount} 金币"

messages:
  death-penalty: "&c你已扣除 {amount} 金币"
  show-no-balance: true
  no-balance: "&e你已身无分文"
```

---

## 🔧 开发环境

- **Java:** 8+
- **Build:** Maven 3.6+
- **API:** Spigot / Paper API 1.16.5
- **测试服务端:** Paper 1.16.5 / Arclight 1.16.5

---

## 🤝 贡献

欢迎 Issue / PR！

1. Fork 本仓库
2. 创建新分支: `git checkout -b feature/awesome`
3. 提交更改: `git commit -m "Add awesome feature"`
4. 推送分支: `git push origin feature/awesome`
5. 发起 Pull Request

---

## ⚖️ License

DeathPenalty 使用 **MIT License**，详见 [LICENSE](LICENSE)。

---

> **制作团队**：曙光团队  
> **制作人**：鳌江
