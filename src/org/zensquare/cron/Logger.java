/*
 * Zensquare Java Cron Library (ZenJCL)
 * 
 * Copyright (C) 2015 Nick Rechten, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *
*/
package org.zensquare.cron;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A basic one file logging system that can be easily extended or replaced with
 * the logging library you are already using.
 * 
 * @author nrechten
 */
public class Logger {

    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
    public static final int CRITICAL = 4;
    public static final int FATAL = 5;

    public static String[] LEVEL_NAMES = new String[]{"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL", "FATAL"};

    private static Logger logger;
    private List<LogOutput> output = new ArrayList<LogOutput>();
    private Map<String, long[]> timestamps = new HashMap<String, long[]>();

    private Logger() {
    }

    public static void init(boolean addDefault) {
        if (logger == null) {
            logger = new Logger();
            if (addDefault) {
                logger._addOutput(new ConsoleLogger());
            }
        }
    }

    private synchronized void modOutput(Object message, Throwable t, int level, LogOutput add, LogOutput remove) {
        if (add != null) {
            if (!output.contains(add)) {
                output.add(add);
            }
        }
        if (message != null || t != null) {
            for (LogOutput logOutput : output) {
                logOutput.log(message, t, level);
            }
        }
        if (remove != null) {
            output.remove(remove);
        }
    }

    protected synchronized void _log(Object message, Throwable t, int level) {
        modOutput(message, t, level, null, null);
    }

    public synchronized static void log(Object message, Throwable t, int level) {
        init(true);
        logger._log(message, t, level);
    }

    public static void info(String message, Throwable t) {
        log(message, t, INFO);
    }

    public static void debug(String message, Throwable t) {
        log(message, t, DEBUG);
    }

    public static void warn(String message, Throwable t) {
        log(message, t, WARNING);
    }

    public static void error(String message, Throwable t) {
        log(message, t, ERROR);
    }

    public static void crit(String message, Throwable t) {
        log(message, t, CRITICAL);
    }

    public static void fatal(String message, Throwable t) {
        log(message, t, FATAL);
    }

    public static void info(String message) {
        log(message, null, INFO);
    }

    public static void debug(String message) {
        log(message, null, DEBUG);
    }

    public static void warn(String message) {
        log(message, null, WARNING);
    }

    public static void error(String message) {
        log(message, null, ERROR);
    }

    public static void crit(String message) {
        log(message, null, CRITICAL);
    }

    public static void fatal(String message) {
        log(message, null, FATAL);
    }

    public static void info(Object message) {
        log(message, null, INFO);
    }

    public static void debug(Object message) {
        log(message, null, DEBUG);
    }

    public static void warn(Object message) {
        log(message, null, WARNING);
    }

    public static void error(Object message) {
        log(message, null, ERROR);
    }

    public static void crit(Object message) {
        log(message, null, CRITICAL);
    }

    public static void fatal(Object message) {
        log(message, null, FATAL);
    }

    public static void info(Throwable t) {
        log(null, t, INFO);
    }

    public static void debug(Throwable t) {
        log(null, t, DEBUG);
    }

    public static void warn(Throwable t) {
        log(null, t, WARNING);
    }

    public static void error(Throwable t) {
        log(null, t, ERROR);
    }

    public static void crit(Throwable t) {
        log(null, t, CRITICAL);
    }

    public static void fatal(Throwable t) {
        log(null, t, FATAL);
    }

    public static void addOutput(LogOutput output) {
        init(false);
        logger._addOutput(output);
    }

    public static void removeOutput(LogOutput output) {
        init(true);
        logger._removeOutput(output);
    }

    private void _addOutput(LogOutput output) {
        modOutput("Added ouput : " + output, null, DEBUG, output, null);
    }

    private void _removeOutput(LogOutput output) {
        modOutput("Removed ouput : " + output, null, DEBUG, null, output);
    }

    public static List<LogOutput> getOutputAdaptors() {
        init(true);
        return logger.output;
    }
    
    public static void timestamp(String message, int level, String prefix, String... keys) {
        StringBuilder sb = new StringBuilder(message);
        boolean first = true;
        long now = System.currentTimeMillis();
        for (String key : keys) {
            if (logger.timestamps.containsKey(key)) {
                if (first) {
                    first = false;
                    sb.append(" - ");
                } else {
                    sb.append(", ");
                }
                long[] ts = logger.timestamps.get(key);
                sb.append(key).append("= d:").append(now - ts[0]).append("ms t:").append(now - ts[1]).append("ms");
            }
        }
        log(sb.toString(), null, level);
    }

    public static void turnOnDebugging(){
        init(true);
        setReportedLevel(DEBUG);
    }
    
    public static void setReportedLevel(int level){
        init(true);
        for (LogOutput logOutput : logger.output) {
            logOutput.lower = level;
        }
    }
    
    /**
     *  Logs messages to the console
     */
    public static class ConsoleLogger extends LogOutput {


        protected OutputFormatter formatter = new OutputFormatter();
 
        public ConsoleLogger() {
        }

        public ConsoleLogger(int lower, int upper) {
            super(lower, upper);
        }

        public void log(Object message, Throwable t, int level) {
            if (level >= lower && level <= upper) {
                if (message != null) {
                    formatter.formatMessage(message, level, level > 2 ? System.err : System.out);
                }
                if (t != null) {
                    formatter.formatThrowable(t, level, level > 2 ? System.err : System.out);
                }
            }
        }
    }

    public static abstract class LogOutput {
        public int lower = INFO;
        public int upper = 100;

        public LogOutput() {
        }

        public LogOutput(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
        }
        
        public abstract void log(Object message, Throwable t, int level);
    }

    /**
     * Adds timestamp and log level to messages, can be extended to format
     * objects if needed.
     * 
     * The project this class was initial written for involved a lot of XML,
     * adding this layer allowed us to provide nice logging output without
     * polluting the main class. An added benefit is that the formatting code is
     * only called if the message is going to be logged.
     */
    public static class OutputFormatter {

        public static String[] LEVEL_NAMES = new String[]{"DEBUG   ", "INFO    ", "WARNING ", "ERROR   ", "CRITICAL", "FATAL   "};

        public void formatMessage(Object message, int level, PrintStream pw) {
            pw.print(System.currentTimeMillis());
            pw.print(":");
            pw.print(LEVEL_NAMES[level]);
            pw.print(":");
            pw.println(message);
        }

        public void formatThrowable(Throwable throwable, int level, PrintStream pw) {
            pw.print(System.currentTimeMillis());
            pw.print(":");
            pw.print(LEVEL_NAMES[level]);
            pw.println(": Exception thrown (see below)");
            throwable.printStackTrace(pw);
        }
    }
}
