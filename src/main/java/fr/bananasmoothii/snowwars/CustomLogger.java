/*
 *    Copyright 2020 ScriptCommands
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fr.bananasmoothii.snowwars;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"UseOfSystemOutOrSystemErr", "unused"})
public class CustomLogger {

    private static @Nullable Logger logger;
    private static boolean logThroughInfo;
    private static @NotNull Level level = Level.INFO;

    private CustomLogger() { }

    public static void log(@NotNull Level logLevel, Object msg) {
        if (logger != null && (!logThroughInfo || logLevel.intValue() >= 800))
            logger.log(logLevel, msg.toString());
        else {
            switch (logLevel.intValue()) {
                case 1000:
                    if (level.intValue() > 1000) break;
                    System.err.println("[ERROR] " + msg);
                    break;
                case 900:
                    if (level.intValue() > 900) break;
                    System.err.println(msg);
                    break;
                case 800:
                    if (level.intValue() > 800) break;
                    System.out.println(msg); // no need for "[INFO]" as it is already there in a minecraft server's logs
                    break;
                case 700:
                    if (level.intValue() > 700) break;
                    System.out.println("[CONFIG] " + msg);
                    break;
                case 500:
                    if (level.intValue() > 500) break;
                    System.out.println("[FINE] " + msg);
                    break;
                case 400:
                    if (level.intValue() > 400) break;
                    System.out.println("[FINER] " + msg);
                    break;
                case 300:
                    if (level.intValue() > 300) break;
                    System.out.println("[FINEST] " + msg);
                    break;
                default:
                    System.out.println("[UNKNOWN IMPORTANCE] " + msg);
            }
        }
    }

    //=======================================================================
    // Start of simple convenience methods using level names as method names
    //=======================================================================

    /**
     * Log a SEVERE message.
     * <p>
     * If the logger is currently enabled for the SEVERE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void severe(Object msg) {
        log(Level.SEVERE, msg);
    }

    /**
     * Log a WARNING message.
     * <p>
     * If the logger is currently enabled for the WARNING message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void warning(Object msg) {
        log(Level.WARNING, msg);
    }

    /**
     * Log an INFO message.
     * <p>
     * If the logger is currently enabled for the INFO message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void info(Object msg) {
        log(Level.INFO, msg);
    }

    /**
     * Log a CONFIG message.
     * <p>
     * If the logger is currently enabled for the CONFIG message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void config(Object msg) {
        log(Level.CONFIG, msg);
    }

    /**
     * Log a FINE message.
     * <p>
     * If the logger is currently enabled for the FINE message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void fine(Object msg) {
        log(Level.FINE, msg);
    }

    /**
     * Log a FINER message.
     * <p>
     * If the logger is currently enabled for the FINER message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void finer(Object msg) {
        log(Level.FINER, msg);
    }

    /**
     * Log a FINEST message.
     * <p>
     * If the logger is currently enabled for the FINEST message
     * level then the given message is forwarded to all the
     * registered output Handler objects.
     * <p>
     * @param   msg     The Object message (or a key in the message catalog)
     */
    public static void finest(Object msg) {
        log(Level.FINEST, msg);
    }


    public static @NotNull Level getLevel() {
        return level;
    }

    public static void setLevel(@NotNull Level level) {
        if (logger != null) logger.setLevel(level);
        CustomLogger.level = level;
    }

    public static boolean isLogThroughInfo() {
        return logThroughInfo;
    }

    public static void setLogThroughInfo(boolean logThroughInfo) {
        CustomLogger.logThroughInfo = logThroughInfo;
    }

    public static @Nullable Logger getLogger() {
        return logger;
    }

    public static void setLogger(@Nullable Logger logger) {
        CustomLogger.logger = logger;
        if (logger != null) level = logger.getLevel();
    }
}
