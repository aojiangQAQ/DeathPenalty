package cn.shuguang.deathpenalty.command;

import cn.shuguang.deathpenalty.DeathPenalty;
import cn.shuguang.deathpenalty.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /deathpenalty 指令处理器
 * <p>
 * 子命令：
 *   reload         - 重载配置
 *   info           - 查看当前所有设置
 *   setmode        - 切换金钱扣款模式 (percent|fixed)
 *   setpct         - 设置金钱百分比
 *   setfixed       - 设置金钱固定金额
 *   money          - 开关金钱惩罚 (on|off)
 *   exp            - 开关经验值惩罚 (on|off)
 *   expmode        - 切换经验扣除模式 (levels|points)
 *   setexppct      - 设置等级百分比 (levels 模式)
 *   setexppoints   - 设置固定经验点数 (points 模式)
 *   test           - 测试扣款（管理员测试用）
 * </p>
 */
public class DeathPenaltyCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "reload", "info",
            "setmode", "setpct", "setfixed", "money",
            "exp", "expmode", "setexppct", "setexppoints",
            "test"
    );

    private final DeathPenalty plugin;

    public DeathPenaltyCommand(DeathPenalty plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("deathpenalty.admin")) {
            sender.sendMessage(MessageUtil.colorize("&c你没有权限使用此指令！"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // ── 通用 ──────────────────────────────────────────────
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(MessageUtil.colorize("&a[DeathPenalty] 配置已重载！"));
                break;

            case "info":
                sendInfo(sender);
                break;

            // ── 金钱惩罚 ──────────────────────────────────────────
            case "money":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp money <on|off>"));
                    return true;
                }
                handleToggle(sender, "penalty.enabled", args[1], "金钱惩罚");
                break;

            case "setmode":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp setmode <percent|fixed>"));
                    return true;
                }
                String newMode = args[1].toLowerCase();
                if (!newMode.equals("percent") && !newMode.equals("fixed")) {
                    sender.sendMessage(MessageUtil.colorize("&c模式只能是 &epercent &c或 &efixed"));
                    return true;
                }
                plugin.getConfig().set("penalty.mode", newMode);
                plugin.saveConfig();
                sender.sendMessage(MessageUtil.colorize("&a金钱扣除模式已设置为：&e" + newMode));
                break;

            case "setpct":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp setpct <0~100>"));
                    return true;
                }
                handleDoubleSet(sender, "penalty.percent", args[1], 0, 100, "金钱扣除百分比", "%");
                break;

            case "setfixed":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp setfixed <金额>"));
                    return true;
                }
                handlePositiveDoubleSet(sender, "penalty.fixed-amount", args[1], "金钱固定扣额", "金币");
                break;

            // ── 经验值惩罚 ────────────────────────────────────────
            case "exp":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp exp <on|off>"));
                    return true;
                }
                handleToggle(sender, "exp-penalty.enabled", args[1], "经验值惩罚");
                break;

            case "expmode":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp expmode <levels|points>"));
                    return true;
                }
                String expMode = args[1].toLowerCase();
                if (!expMode.equals("levels") && !expMode.equals("points")) {
                    sender.sendMessage(MessageUtil.colorize("&c模式只能是 &elevels &c（按等级%）或 &epoints &c（固定经验点）"));
                    return true;
                }
                plugin.getConfig().set("exp-penalty.mode", expMode);
                plugin.saveConfig();
                sender.sendMessage(MessageUtil.colorize("&a经验扣除模式已设置为：&b" + expMode));
                break;

            case "setexppct":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp setexppct <0~100>"));
                    return true;
                }
                handleDoubleSet(sender, "exp-penalty.level-percent", args[1], 0, 100, "经验等级扣除百分比", "%");
                break;

            case "setexppoints":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp setexppoints <经验点数>"));
                    return true;
                }
                handlePositiveIntSet(sender, "exp-penalty.fixed-points", args[1], "固定扣除经验点数", "点");
                break;

            // ── 测试 ──────────────────────────────────────────────
            case "test":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.colorize("&c用法：/dp test <玩家名>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(MessageUtil.colorize("&c玩家 &e" + args[1] + " &c不在线！"));
                    return true;
                }
                simulateDeduct(sender, target);
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("deathpenalty.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setmode":
                    return Arrays.asList("percent", "fixed").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "expmode":
                    return Arrays.asList("levels", "points").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "money":
                case "exp":
                    return Arrays.asList("on", "off").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                case "test":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    // ── 私有方法 ──────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        String[] lines = {
                "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&6&lDeathPenalty &7v" + plugin.getDescription().getVersion() + " &8| &7曙光团队 · 鳌江",
                "&8&l─── &e金钱惩罚 &8&l───────────────────────────",
                "&e/dp money &b<on|off>           &7- 开关金钱惩罚",
                "&e/dp setmode &b<percent|fixed>  &7- 切换金钱模式",
                "&e/dp setpct  &b<0-100>          &7- 设置金钱百分比",
                "&e/dp setfixed &b<金额>           &7- 设置固定扣额",
                "&8&l─── &b经验值惩罚 &8&l─────────────────────────",
                "&b/dp exp &3<on|off>              &7- 开关经验值惩罚",
                "&b/dp expmode &3<levels|points>   &7- 切换经验模式",
                "&b/dp setexppct &3<0-100>         &7- 设置等级扣除%",
                "&b/dp setexppoints &3<数值>       &7- 设置固定经验点数",
                "&8&l─── &7通用 &8&l──────────────────────────────",
                "&7/dp reload                   &7- 重载配置文件",
                "&7/dp info                     &7- 查看当前全部设置",
                "&7/dp test &8<玩家名>             &7- 模拟扣款测试",
                "&8&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        };
        for (String line : lines) {
            sender.sendMessage(MessageUtil.colorize(line));
        }
    }

    private void sendInfo(CommandSender sender) {
        boolean moneyEnabled = plugin.getConfig().getBoolean("penalty.enabled", true);
        String moneyMode = plugin.getConfig().getString("penalty.mode", "percent");
        double pct = plugin.getConfig().getDouble("penalty.percent", 15.0);
        double fixed = plugin.getConfig().getDouble("penalty.fixed-amount", 1000.0);

        boolean expEnabled = plugin.getConfig().getBoolean("exp-penalty.enabled", true);
        String expMode = plugin.getConfig().getString("exp-penalty.mode", "levels");
        double expPct = plugin.getConfig().getDouble("exp-penalty.level-percent", 20.0);
        int expPoints = plugin.getConfig().getInt("exp-penalty.fixed-points", 100);

        boolean broadcast = plugin.getConfig().getBoolean("broadcast.enabled", false);
        boolean expBroadcast = plugin.getConfig().getBoolean("broadcast.exp-enabled", false);

        sender.sendMessage(MessageUtil.colorize("&8&l━━━━ &6DeathPenalty &7v" + plugin.getDescription().getVersion() + " 当前配置 &8&l━━━━"));
        sender.sendMessage(MessageUtil.colorize("&e【金钱惩罚】 " + (moneyEnabled ? "&a开启" : "&c关闭")));
        sender.sendMessage(MessageUtil.colorize("  &7扣款模式：&e" + moneyMode
                + "  &7| 百分比：&e" + pct + "%  &7| 固定额：&e" + fixed));
        sender.sendMessage(MessageUtil.colorize("  &7全服广播：&e" + (broadcast ? "开启" : "关闭")
                + "  &7| 经济插件：&e" + plugin.getEconomy().getName()));
        sender.sendMessage(MessageUtil.colorize("&b【经验值惩罚】 " + (expEnabled ? "&a开启" : "&c关闭")));
        sender.sendMessage(MessageUtil.colorize("  &7扣除模式：&b" + expMode
                + "  &7| 等级%：&b" + expPct + "%  &7| 固定点：&b" + expPoints));
        sender.sendMessage(MessageUtil.colorize("  &7全服广播：&b" + (expBroadcast ? "开启" : "关闭")));
    }

    private void handleToggle(CommandSender sender, String configKey, String arg, String featureName) {
        if (arg.equalsIgnoreCase("on")) {
            plugin.getConfig().set(configKey, true);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.colorize("&a" + featureName + " 已 &l开启&r&a！"));
        } else if (arg.equalsIgnoreCase("off")) {
            plugin.getConfig().set(configKey, false);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.colorize("&c" + featureName + " 已 &l关闭&r&c！"));
        } else {
            sender.sendMessage(MessageUtil.colorize("&c只能输入 &eon &c或 &eoff"));
        }
    }

    private void handleDoubleSet(CommandSender sender, String configKey, String input,
                                  double min, double max, String label, String unit) {
        try {
            double val = Double.parseDouble(input);
            if (val < min || val > max) throw new NumberFormatException();
            plugin.getConfig().set(configKey, val);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.colorize("&a" + label + " 已设置为：&e" + val + unit));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&c请输入 " + min + "~" + max + " 之间的数字！"));
        }
    }

    private void handlePositiveDoubleSet(CommandSender sender, String configKey, String input,
                                          String label, String unit) {
        try {
            double val = Double.parseDouble(input);
            if (val < 0) throw new NumberFormatException();
            plugin.getConfig().set(configKey, val);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.colorize("&a" + label + " 已设置为：&e" + val + " " + unit));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&c请输入正数！"));
        }
    }

    private void handlePositiveIntSet(CommandSender sender, String configKey, String input,
                                       String label, String unit) {
        try {
            int val = Integer.parseInt(input);
            if (val < 0) throw new NumberFormatException();
            plugin.getConfig().set(configKey, val);
            plugin.saveConfig();
            sender.sendMessage(MessageUtil.colorize("&a" + label + " 已设置为：&b" + val + " " + unit));
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.colorize("&c请输入非负整数！"));
        }
    }

    private void simulateDeduct(CommandSender sender, Player target) {
        sender.sendMessage(MessageUtil.colorize("&8--- &7模拟扣款测试：&e" + target.getName() + " &8---"));

        // 模拟金钱扣除
        boolean moneyEnabled = plugin.getConfig().getBoolean("penalty.enabled", true);
        if (moneyEnabled) {
            double balance = plugin.getEconomy().getBalance(target);
            if (balance > 0) {
                String mode = plugin.getConfig().getString("penalty.mode", "percent");
                double amount;
                if ("percent".equalsIgnoreCase(mode)) {
                    amount = balance * (plugin.getConfig().getDouble("penalty.percent", 15.0) / 100.0);
                } else {
                    amount = Math.min(plugin.getConfig().getDouble("penalty.fixed-amount", 1000.0), balance);
                }
                amount = Math.round(amount * 100.0) / 100.0;
                plugin.getEconomy().withdrawPlayer(target, amount);
                double newBalance = plugin.getEconomy().getBalance(target);
                sender.sendMessage(MessageUtil.colorize("&e[金钱] 扣除 &c" + String.format("%.2f", amount)
                        + " &e，剩余：&c" + String.format("%.2f", newBalance)));
            } else {
                sender.sendMessage(MessageUtil.colorize("&7[金钱] 余额为零，跳过。"));
            }
        } else {
            sender.sendMessage(MessageUtil.colorize("&7[金钱] 已关闭，跳过。"));
        }

        // 模拟经验扣除（仅提示，不实际扣除）
        boolean expEnabled = plugin.getConfig().getBoolean("exp-penalty.enabled", true);
        if (expEnabled) {
            int level = target.getLevel();
            String expMode = plugin.getConfig().getString("exp-penalty.mode", "levels");
            if ("levels".equalsIgnoreCase(expMode)) {
                double pct = plugin.getConfig().getDouble("exp-penalty.level-percent", 20.0);
                int deduct = (int) Math.ceil(level * (pct / 100.0));
                deduct = Math.min(deduct, level);
                sender.sendMessage(MessageUtil.colorize("&b[经验] 预计扣除 &3" + deduct
                        + " 级（当前 " + level + " 级，扣 " + pct + "%）&8（仅预览，不实际扣除）"));
            } else {
                int pts = plugin.getConfig().getInt("exp-penalty.fixed-points", 100);
                sender.sendMessage(MessageUtil.colorize("&b[经验] 预计扣除 &3" + pts
                        + " 经验点（当前 " + level + " 级）&8（仅预览，不实际扣除）"));
            }
        } else {
            sender.sendMessage(MessageUtil.colorize("&7[经验] 已关闭，跳过。"));
        }
    }
}
