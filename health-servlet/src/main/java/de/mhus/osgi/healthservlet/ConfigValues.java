/**
 * Copyright (C) 2020 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.osgi.healthservlet;

import java.util.HashSet;
import java.util.regex.Pattern;

import de.mhus.osgi.healthservlet.HealthServlet.Config;

public class ConfigValues {

    // public Properties props;

    public long waitAfterStart;
    public HashSet<String> bundlesIgnore;
    public boolean bundlesEnabled;
    public boolean logEnabled;
    public int logLevel;
    public HashSet<Pattern> logPatterns;
    public boolean logResetFinding;
    public boolean checkEnabled;
    public HashSet<String> checkIgnore;
    public boolean checkCombineTagsWithOr;
    public boolean checkForceInstantExecution;
    public String checkOverrideGlobalTimeoutStr;
    public String checkTags;

    public ConfigValues(Config c) {
        waitAfterStart = c.waitAfterStart();
        bundlesIgnore = new HashSet<>();
        for (String e : c.bundlesIgnore()) bundlesIgnore.add(e);
        bundlesEnabled = c.bundlesEnabled();
        logEnabled = c.logEnabled();
        logLevel = c.logLevel().toInt();
        logPatterns = new HashSet<Pattern>();
        for (String e : c.logPatterns()) {
            try {
                logPatterns.add(Pattern.compile(e, Pattern.DOTALL));
            } catch (Throwable t) {
            }
        }
        logResetFinding = c.logResetFinding();
        checkEnabled = c.checkEnabled();
        checkIgnore = new HashSet<String>();
        for (String e : c.checkIgnore()) checkIgnore.add(e);
        checkCombineTagsWithOr = c.checkCombineTagsWithOr();
        checkForceInstantExecution = c.checkForceInstantExecution();
        checkOverrideGlobalTimeoutStr = c.checkOverrideGlobalTimeoutStr();
        checkTags = c.checkTags();
    }

    public ConfigValues(de.mhus.osgi.healthservlet.ReadyServlet.Config c) {
        bundlesIgnore = new HashSet<>();
        for (String e : c.bundlesIgnore()) bundlesIgnore.add(e);
        bundlesEnabled = c.bundlesEnabled();
        logEnabled = c.logEnabled();
        logLevel = c.logLevel().toInt();
        logPatterns = new HashSet<Pattern>();
        for (String e : c.logPatterns()) {
            try {
                logPatterns.add(Pattern.compile(e, Pattern.DOTALL));
            } catch (Throwable t) {
            }
        }
        logResetFinding = c.logResetFinding();
        checkEnabled = c.checkEnabled();
        checkIgnore = new HashSet<String>();
        for (String e : c.checkIgnore()) checkIgnore.add(e);
        checkCombineTagsWithOr = c.checkCombineTagsWithOr();
        checkForceInstantExecution = c.checkForceInstantExecution();
        checkOverrideGlobalTimeoutStr = c.checkOverrideGlobalTimeoutStr();
        checkTags = c.checkTags();
    }

    //
    //    public ConfigTemplate(ComponentContext ctx, Logger log) {
    //        props = new Properties();
    //        Dictionary<String, Object> properties = ctx.getProperties();
    //        Enumeration<String> keys = properties.keys();
    //        while (keys.hasMoreElements()) {
    //            String key = keys.nextElement();
    //            props.put(key, properties.get(key));
    //        }
    //
    //        waitAfterStart = Long.parseLong(props.getProperty("waitAfterStart", "60000"));
    //
    //        // bundles
    //        bundlesEnabled = Boolean.parseBoolean(props.getProperty("bundles.enabled", "true"));
    //        bundlesIgnore = new HashSet<>();
    //        for (String part : props.getProperty("bundles.ignore", "").split(",")) {
    //            part = part.trim();
    //            if (part.length() > 0)
    //                bundlesIgnore.add(part);
    //        }
    //        if (bundlesIgnore.size() == 0) {
    //            bundlesIgnore.add("org.apache.karaf.features.extension");
    //            bundlesIgnore.add("org.apache.aries.blueprint.core.compatibility");
    //            bundlesIgnore.add("org.apache.karaf.shell.console");
    //            bundlesIgnore.add("org.jline.terminal-jansi");
    //        }
    //
    //        // log messages
    //        logEnabled = Boolean.parseBoolean(props.getProperty("log.enabled", "false"));
    //        logLevel = getMinLevel(props.getProperty("log.level", "DEBUG"));
    //        logResetFinding = Boolean.parseBoolean(props.getProperty("log.resetFindings",
    // "true"));
    //        logPatterns = new HashSet<>();
    //        for (Object nameO : props.keySet()) {
    //            String name = nameO.toString();
    //            if (name.startsWith("log.pattern.")) {
    //                try {
    //                    logPatterns.add( Pattern.compile(props.getProperty(name), Pattern.DOTALL)
    // );
    //                } catch (Throwable t) {
    //                    log.warning("Log Pattern Fails: " + name);
    //                }
    //            }
    //        }
    //        if (logPatterns.size() == 0) {
    //            logPatterns.add(Pattern.compile(".* java\\.lang\\.OutOfMemoryError:.*"));
    //        }
    //
    //        // health check
    //        checkEnabled = Boolean.parseBoolean(props.getProperty("check.enabled", "true"));
    //        checkIgnore = new HashSet<>();
    //        for (String part : props.getProperty("check.ignore", "").split(",")) {
    //            part = part.trim();
    //            if (part.length() > 0)
    //                checkIgnore.add(part);
    //        }
    //        checkCombineTagsWithOr =
    // Boolean.parseBoolean(props.getProperty("check.combineTagsWithOr", "false"));
    //        checkForceInstantExecution =
    // Boolean.parseBoolean(props.getProperty("check.forceInstantExecution", "false"));
    //        checkOverrideGlobalTimeoutStr = props.getProperty("check.overrideGlobalTimeoutStr",
    // "");
    //        checkTags = props.getProperty("check.tags", "*");
    //
    //
    //    }
    //
    //    public static int getMinLevel(String levelSt) {
    //        int minLevel = Integer.MAX_VALUE;
    //        if (levelSt != null) {
    //            switch (levelSt.toLowerCase()) {
    //                case "debug":
    //                    minLevel = DEBUG_INT;
    //                    break;
    //                case "info":
    //                    minLevel = INFO_INT;
    //                    break;
    //                case "warn":
    //                    minLevel = WARN_INT;
    //                    break;
    //                case "error":
    //                    minLevel = ERROR_INT;
    //                    break;
    //                case "all":
    //                    minLevel = ALL_INT;
    //                    break;
    //            }
    //        }
    //        return minLevel;
    //    }

}
