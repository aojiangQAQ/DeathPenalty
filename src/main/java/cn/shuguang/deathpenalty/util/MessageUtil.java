package cn.shuguang.deathpenalty.util;

import org.bukkit.ChatColor;

/**
 * 消息工具类，提供颜色代码转换
 */
public final class MessageUtil {

    private MessageUtil() {}

    /**
     * 将 & 颜色符号转换为 Minecraft 颜色代码
     *
     * @param message 原始消息
     * @return 带颜色的消息
     */
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
