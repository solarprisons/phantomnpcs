package prisons.solar.npclib.paper.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized logging utility for Phantom NPC library.
 * Uses the plugin's logger for proper log formatting and configuration.
 *
 * <p>Must be initialized with {@link #init(JavaPlugin)} before use.
 * All methods are null-safe and will silently no-op if not initialized.
 */
public final class PhantomLogger {

    private static volatile Logger logger;
    private static volatile boolean debugEnabled = false;

    private PhantomLogger() {
        // Utility class
    }

    /**
     * Initializes the logger with the plugin instance.
     * Should be called during plugin enable.
     *
     * @param plugin the plugin instance
     */
    public static void init(@NotNull JavaPlugin plugin) {
        logger = plugin.getLogger();
    }

    /**
     * Initializes the logger with debug mode.
     *
     * @param plugin the plugin instance
     * @param debug  whether debug logging is enabled
     */
    public static void init(@NotNull JavaPlugin plugin, boolean debug) {
        logger = plugin.getLogger();
        debugEnabled = debug;
    }

    /**
     * Sets whether debug logging is enabled.
     *
     * @param enabled true to enable debug logging
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    /**
     * Checks if debug logging is enabled.
     *
     * @return true if debug logging is enabled
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public static void info(@NotNull String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    /**
     * Logs an info message with formatting.
     *
     * @param format the format string
     * @param args   the format arguments
     */
    public static void info(@NotNull String format, @Nullable Object... args) {
        if (logger != null) {
            logger.info(String.format(format, args));
        }
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public static void warning(@NotNull String message) {
        if (logger != null) {
            logger.warning(message);
        }
    }

    /**
     * Logs a warning message with formatting.
     *
     * @param format the format string
     * @param args   the format arguments
     */
    public static void warning(@NotNull String format, @Nullable Object... args) {
        if (logger != null) {
            logger.warning(String.format(format, args));
        }
    }

    /**
     * Logs a severe/error message.
     *
     * @param message the message
     */
    public static void severe(@NotNull String message) {
        if (logger != null) {
            logger.severe(message);
        }
    }

    /**
     * Logs a severe/error message with formatting.
     *
     * @param format the format string
     * @param args   the format arguments
     */
    public static void severe(@NotNull String format, @Nullable Object... args) {
        if (logger != null) {
            logger.severe(String.format(format, args));
        }
    }

    /**
     * Logs a severe/error message with an exception.
     *
     * @param message   the message
     * @param throwable the exception
     */
    public static void severe(@NotNull String message, @NotNull Throwable throwable) {
        if (logger != null) {
            logger.log(Level.SEVERE, message, throwable);
        }
    }

    /**
     * Logs a debug message (only if debug mode is enabled).
     *
     * @param message the message
     */
    public static void debug(@NotNull String message) {
        if (logger != null && debugEnabled) {
            logger.info("[DEBUG] " + message);
        }
    }

    /**
     * Logs a debug message with formatting (only if debug mode is enabled).
     *
     * @param format the format string
     * @param args   the format arguments
     */
    public static void debug(@NotNull String format, @Nullable Object... args) {
        if (logger != null && debugEnabled) {
            logger.info("[DEBUG] " + String.format(format, args));
        }
    }

    /**
     * Shuts down the logger.
     * Should be called during plugin disable.
     */
    public static void shutdown() {
        logger = null;
        debugEnabled = false;
    }
}
