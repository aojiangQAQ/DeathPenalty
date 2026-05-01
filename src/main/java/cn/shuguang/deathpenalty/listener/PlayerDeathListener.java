package cn.shuguang.deathpenalty.listener;

import cn.shuguang.deathpenalty.DeathPenalty;
import cn.shuguang.deathpenalty.util.MessageUtil;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * 玩家死亡事件监听器
 * <p>支持百分比扣除和固定金额两种金钱模式</p>
 * <p>支持按等级百分比扣除和固定经验点数两种经验模式</p>
 */
public class PlayerDeathListener implements Listener {

    private final DeathPenalty plugin;

    public PlayerDeathListener(DeathPenalty plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // ── 金钱惩罚 ──────────────────────────────────────────
        boolean moneyEnabled = plugin.getConfig().getBoolean("penalty.enabled", true);
        if (moneyEnabled && !player.hasPermission("deathpenalty.bypass")) {
            handleMoneyPenalty(player);
        }

        // ── 经验值惩罚 ────────────────────────────────────────
        boolean expEnabled = plugin.getConfig().getBoolean("exp-penalty.enabled", true);
        if (expEnabled && !player.hasPermission("deathpenalty.bypass-exp")) {
            handleExpPenalty(player);
        }
    }

    // ── 金钱惩罚逻辑 ──────────────────────────────────────────

    private void handleMoneyPenalty(Player player) {
        Economy economy = plugin.getEconomy();
        double balance = economy.getBalance(player);

        if (balance <= 0) {
            sendNoBalanceMessage(player);
            return;
        }

        String mode = plugin.getConfig().getString("penalty.mode", "percent");
        double deductAmount;

        if ("percent".equalsIgnoreCase(mode)) {
            double percent = plugin.getConfig().getDouble("penalty.percent", 15.0);
            percent = Math.max(0, Math.min(100, percent));
            deductAmount = balance * (percent / 100.0);
        } else {
            deductAmount = plugin.getConfig().getDouble("penalty.fixed-amount", 1000.0);
            deductAmount = Math.min(deductAmount, balance);
        }

        deductAmount = Math.round(deductAmount * 100.0) / 100.0;

        if (deductAmount <= 0) {
            return;
        }

        EconomyResponse response = economy.withdrawPlayer(player, deductAmount);

        if (response.transactionSuccess()) {
            double newBalance = economy.getBalance(player);
            sendDeductMessage(player, deductAmount, newBalance);

            if (plugin.getConfig().getBoolean("broadcast.enabled", false)) {
                String broadcastMsg = MessageUtil.colorize(
                        plugin.getConfig().getString("broadcast.message",
                                "&7[死亡惩罚] &e{player} &7死亡被扣除 &c{amount} &7金币！")
                                .replace("{player}", player.getName())
                                .replace("{amount}", String.format("%.2f", deductAmount))
                );
                plugin.getServer().broadcastMessage(broadcastMsg);
            }

            plugin.getLogger().info(player.getName() + " 死亡，扣除金钱 " + deductAmount
                    + "，剩余余额：" + String.format("%.2f", newBalance));
        } else {
            plugin.getLogger().warning("扣款失败：" + player.getName()
                    + " | 原因：" + response.errorMessage);
        }
    }

    // ── 经验值惩罚逻辑 ────────────────────────────────────────

    private void handleExpPenalty(Player player) {
        int currentLevel = player.getLevel();
        // getTotalExperience() 返回全部经验点（含当前等级进度）
        int totalExp = getTotalExp(player);

        if (totalExp <= 0 || currentLevel <= 0) {
            sendNoExpMessage(player);
            return;
        }

        String mode = plugin.getConfig().getString("exp-penalty.mode", "levels");
        int deductLevels = 0;   // levels 模式
        int deductPoints = 0;   // points 模式
        String typeLabel;

        if ("levels".equalsIgnoreCase(mode)) {
            double levelPct = plugin.getConfig().getDouble("exp-penalty.level-percent", 20.0);
            levelPct = Math.max(0, Math.min(100, levelPct));
            // 至少扣 1 级（有等级时）
            deductLevels = (int) Math.ceil(currentLevel * (levelPct / 100.0));
            deductLevels = Math.min(deductLevels, currentLevel);
            typeLabel = "级";

            if (deductLevels <= 0) {
                return;
            }

            // 直接修改等级
            boolean allowZero = plugin.getConfig().getBoolean("exp-penalty.allow-zero", true);
            int newLevel = currentLevel - deductLevels;
            if (!allowZero) {
                newLevel = Math.max(0, newLevel);
                deductLevels = currentLevel - newLevel;
            }
            player.setLevel(Math.max(0, newLevel));
            // 清零当前等级内的进度（可选，保持一致）
            if (player.getLevel() == 0) {
                player.setExp(0f);
            }

            int finalLevel = player.getLevel();
            sendExpDeductMessage(player, deductLevels, typeLabel, finalLevel);

            if (plugin.getConfig().getBoolean("broadcast.exp-enabled", false)) {
                broadcastExpMessage(player, deductLevels, typeLabel);
            }

            plugin.getLogger().info(player.getName() + " 死亡，扣除 " + deductLevels
                    + " 级，当前等级：" + finalLevel);

        } else {
            // points 模式
            deductPoints = plugin.getConfig().getInt("exp-penalty.fixed-points", 100);
            deductPoints = Math.max(0, deductPoints);
            typeLabel = "经验点";

            if (deductPoints <= 0) {
                return;
            }

            // 计算扣除后的总经验，不低于 0
            int newTotalExp = Math.max(0, totalExp - deductPoints);
            int actualDeduct = totalExp - newTotalExp;

            if (actualDeduct <= 0) {
                return;
            }

            // 重置经验
            setTotalExp(player, newTotalExp);

            sendExpDeductMessage(player, actualDeduct, typeLabel, player.getLevel());

            if (plugin.getConfig().getBoolean("broadcast.exp-enabled", false)) {
                broadcastExpMessage(player, actualDeduct, typeLabel);
            }

            plugin.getLogger().info(player.getName() + " 死亡，扣除 " + actualDeduct
                    + " 经验点，当前等级：" + player.getLevel());
        }
    }

    /**
     * 计算玩家总经验点数（Bukkit 原生 getTotalExperience 可能有 bug，手动计算更准确）
     */
    private int getTotalExp(Player player) {
        int level = player.getLevel();
        int total = getExpToLevel(level);
        // getExpForNextLevel(level) 返回当前等级升级所需总经验
        int expToNext = getExpForNextLevel(level);
        total += Math.round(player.getExp() * expToNext);
        return total;
    }

    /**
     * 从 0 级升到指定等级所需总经验
     */
    private int getExpToLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    /**
     * 将玩家总经验设置为指定点数
     */
    private void setTotalExp(Player player, int exp) {
        player.setExp(0f);
        player.setLevel(0);
        player.setTotalExperience(0);

        // 逐步升级，找到对应等级
        int level = 0;
        while (exp >= getExpForNextLevel(level)) {
            exp -= getExpForNextLevel(level);
            level++;
        }
        player.setLevel(level);
        if (getExpForNextLevel(level) > 0) {
            player.setExp((float) exp / getExpForNextLevel(level));
        }
    }

    /**
     * 当前等级升到下一级所需经验
     */
    private int getExpForNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        } else if (level <= 30) {
            return 5 * level - 38;
        } else {
            return 9 * level - 158;
        }
    }

    // ── 私有消息方法 ──────────────────────────────────────────

    private void sendDeductMessage(Player player, double amount, double newBalance) {
        String template = plugin.getConfig().getString(
                "messages.death-penalty",
                "&c你死亡了！扣除 &e{amount} &c金币，当前余额：&e{balance}");
        String msg = MessageUtil.colorize(template
                .replace("{amount}", String.format("%.2f", amount))
                .replace("{balance}", String.format("%.2f", newBalance)));
        player.sendMessage(msg);
    }

    private void sendNoBalanceMessage(Player player) {
        if (!plugin.getConfig().getBoolean("messages.show-no-balance", true)) {
            return;
        }
        String template = plugin.getConfig().getString(
                "messages.no-balance",
                "&7你死亡了，但余额为零，无法扣除金币。");
        player.sendMessage(MessageUtil.colorize(template));
    }

    private void sendExpDeductMessage(Player player, int expDeducted, String typeLabel, int currentLevel) {
        String template = plugin.getConfig().getString(
                "messages.death-exp-penalty",
                "&9你死亡了！扣除 &b{exp} &9{type}，当前等级：&b{level}");
        String msg = MessageUtil.colorize(template
                .replace("{exp}", String.valueOf(expDeducted))
                .replace("{type}", typeLabel)
                .replace("{level}", String.valueOf(currentLevel)));
        player.sendMessage(msg);
    }

    private void sendNoExpMessage(Player player) {
        if (!plugin.getConfig().getBoolean("messages.show-no-exp", true)) {
            return;
        }
        String template = plugin.getConfig().getString(
                "messages.no-exp",
                "&7你死亡了，但经验值为零，无法扣除经验。");
        player.sendMessage(MessageUtil.colorize(template));
    }

    private void broadcastExpMessage(Player player, int expDeducted, String typeLabel) {
        String broadcastMsg = MessageUtil.colorize(
                plugin.getConfig().getString("broadcast.exp-message",
                        "&7[死亡惩罚] &e{player} &7死亡被扣除 &b{exp} &7{type}！")
                        .replace("{player}", player.getName())
                        .replace("{exp}", String.valueOf(expDeducted))
                        .replace("{type}", typeLabel)
        );
        plugin.getServer().broadcastMessage(broadcastMsg);
    }
}
