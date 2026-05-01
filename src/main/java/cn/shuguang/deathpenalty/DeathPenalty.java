package cn.shuguang.deathpenalty;

import cn.shuguang.deathpenalty.command.DeathPenaltyCommand;
import cn.shuguang.deathpenalty.listener.PlayerDeathListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DeathPenalty - 死亡经济惩罚插件
 *
 * <p>曙光团队出品 | 制作人：鳌江</p>
 * <p>兼容：Paper/Arclight 1.16.5 + Vault 经济插件</p>
 * <p>v1.1.0：新增死亡经验值惩罚功能，可独立开关，支持等级百分比和固定点数两种模式</p>
 */
public final class DeathPenalty extends JavaPlugin {

    private static DeathPenalty instance;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 检测并挂钩 Vault 经济
        if (!setupEconomy()) {
            getLogger().severe("未找到 Vault 或经济插件！DeathPenalty 将禁用。");
            getLogger().severe("请确保已安装 Vault 以及一个经济插件（如 EssentialsX）。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);

        // 注册指令
        DeathPenaltyCommand cmdExecutor = new DeathPenaltyCommand(this);
        getCommand("deathpenalty").setExecutor(cmdExecutor);
        getCommand("deathpenalty").setTabCompleter(cmdExecutor);

        getLogger().info("╔═══════════════════════════════════════╗");
        getLogger().info("║   DeathPenalty v" + getDescription().getVersion() + " 已启动        ║");
        getLogger().info("║   制作团队：曙光团队                  ║");
        getLogger().info("║   制作人  ：鳌江                      ║");
        getLogger().info("╚═══════════════════════════════════════╝");
        getLogger().info("经济插件已连接：" + economy.getName());
        printPenaltyMode();
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathPenalty 已卸载。");
    }

    // ── 私有方法 ──────────────────────────────────────────────

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    private void printPenaltyMode() {
        // 金钱模式
        boolean moneyEnabled = getConfig().getBoolean("penalty.enabled", true);
        String mode = getConfig().getString("penalty.mode", "percent");
        if (moneyEnabled) {
            if ("percent".equalsIgnoreCase(mode)) {
                double pct = getConfig().getDouble("penalty.percent", 15.0);
                getLogger().info("金钱惩罚：开启 | 按余额 " + pct + "% 扣除");
            } else {
                double fixed = getConfig().getDouble("penalty.fixed-amount", 1000.0);
                getLogger().info("金钱惩罚：开启 | 固定扣除 " + fixed);
            }
        } else {
            getLogger().info("金钱惩罚：关闭");
        }
        // 经验模式
        boolean expEnabled = getConfig().getBoolean("exp-penalty.enabled", true);
        String expMode = getConfig().getString("exp-penalty.mode", "levels");
        if (expEnabled) {
            if ("levels".equalsIgnoreCase(expMode)) {
                double expPct = getConfig().getDouble("exp-penalty.level-percent", 20.0);
                getLogger().info("经验惩罚：开启 | 按等级 " + expPct + "% 扣除");
            } else {
                int pts = getConfig().getInt("exp-penalty.fixed-points", 100);
                getLogger().info("经验惩罚：开启 | 固定扣除 " + pts + " 经验点");
            }
        } else {
            getLogger().info("经验惩罚：关闭");
        }
    }

    // ── 公开 Getter ───────────────────────────────────────────

    public static DeathPenalty getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }
}
