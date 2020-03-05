/**
 * Copyright 2018 Mike Hummel
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.osgi.healthservlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.karaf.log.core.LogService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Component(
        service = Servlet.class,
        property = "alias=/ready/*",
        name = "ReadyServlet",
        servicefactory = true)
public class ReadyServlet extends HttpServlet {

    public static final int ERROR_INT = 3;
    public static final int WARN_INT = 4;
    public static final int INFO_INT = 6;
    public static final int DEBUG_INT = 7;
    public static final int ALL_INT = 100;
    
    private static final long serialVersionUID = 1L;
    private Properties props;
    private ComponentContext ctx;
    private HashSet<String> bundlesIgnore;
    private LogServiceTracker tracker;
    private boolean bundlesEnabled;
    private boolean logEnabled;
    private int logLevel;
    private HashSet<Pattern> logPatterns;
    private Set<String>  logFindings = Collections.synchronizedSet(new HashSet<>());
    private boolean logResetFinding;
    private boolean checkEnabled;
    private HashSet<String> checkIgnore;
    private static Logger log = Logger.getLogger(ReadyServlet.class.getCanonicalName());

    private HealthCheckExecutor healthCheckExecutor;
    private boolean checkCombineTagsWithOr;
    private boolean checkForceInstantExecution;
    private String checkOverrideGlobalTimeoutStr;
    private String checkTags;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setHealthCheckExecutor(HealthCheckExecutor healthCheckExecutor) {
        log.info("Found healthCheckExecutor");
        this.healthCheckExecutor = healthCheckExecutor;
    }
    
    @Activate
    public void activate(ComponentContext ctx) {
        this.ctx = ctx;
        props = new Properties();
        File f = new File("etc/readyservlet.properties");
        if (f.exists()) {
            log.info("Load config file " + f);
            try {
                FileInputStream is = new FileInputStream(f);
                props.load(is);
                is.close();
            } catch (IOException e) {
                log.warning(e.toString());
            }
        } else {
            log.warning("Config file not found");
        }
        
        // bundles
        bundlesEnabled = Boolean.parseBoolean(props.getProperty("bundles.enabled", "true"));
        bundlesIgnore = new HashSet<>();
        for (String part : props.getProperty("bundles.ignore", "").split(",")) {
            part = part.trim();
            if (part.length() > 0)
                bundlesIgnore.add(part);
        }
        if (bundlesIgnore.size() == 0) {
            bundlesIgnore.add("org.apache.karaf.features.extension");
            bundlesIgnore.add("org.apache.aries.blueprint.core.compatibility");
            bundlesIgnore.add("org.apache.karaf.shell.console");
            bundlesIgnore.add("org.jline.terminal-jansi");
        }
        
        // log messages
        logEnabled = Boolean.parseBoolean(props.getProperty("log.enabled", "false"));
        logLevel = getMinLevel(props.getProperty("log.level", "DEBUG"));
        logResetFinding = Boolean.parseBoolean(props.getProperty("log.resetFindings", "true"));
        logPatterns = new HashSet<>();
        for (Object nameO : props.keySet()) {
            String name = nameO.toString();
            if (name.startsWith("log.pattern.")) {
                try {
                    logPatterns.add( Pattern.compile(props.getProperty(name), Pattern.DOTALL) );
                } catch (Throwable t) {
                    log.warning("Log Pattern Fails: " + name);
                }
            }
        }
        if (logPatterns.size() == 0) {
            logPatterns.add(Pattern.compile(".* java\\.lang\\.OutOfMemoryError:.*"));
        }
        
        // health check
        checkEnabled = Boolean.parseBoolean(props.getProperty("check.enabled", "true"));
        checkIgnore = new HashSet<>();
        for (String part : props.getProperty("check.ignore", "").split(",")) {
            part = part.trim();
            if (part.length() > 0)
                checkIgnore.add(part);
        }
        checkCombineTagsWithOr = Boolean.parseBoolean(props.getProperty("check.combineTagsWithOr", "false"));
        checkForceInstantExecution = Boolean.parseBoolean(props.getProperty("check.forceInstantExecution", "false"));
        checkOverrideGlobalTimeoutStr = props.getProperty("check.overrideGlobalTimeoutStr", "");
        checkTags = props.getProperty("check.tags", "*");

        if (logEnabled) {
            PaxAppender appender = event -> printEvent(event);
            tracker = new LogServiceTracker(ctx.getBundleContext(), LogService.class, null, appender);
            tracker.open();
        }
    }

    @Deactivate
    public void deactivate(ComponentContext ctx) {
        if (tracker != null)
            tracker.close();
        tracker = null;
        this.ctx = null;
    }

    public ReadyServlet() {}

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        boolean healthy = true;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        long time = System.currentTimeMillis();
        out.println("time: " + time + " " + new Date(time));
        // check if bundles are ok
        if (bundlesEnabled) {
            for (Bundle bundle : ctx.getBundleContext().getBundles()) {
                if (bundle.getState() != Bundle.ACTIVE) {
                    if (bundlesIgnore.contains(bundle.getSymbolicName()))
                        continue;
                    out.println("Bundle: " + bundle.getSymbolicName());
                    healthy = false;
                }
            }
        }
        
        // check log
        if (logEnabled && logFindings.size() > 0) {
            healthy = false;
            for (String finding : logFindings)
                out.println("Log: " + finding);
            if (logResetFinding)
                logFindings.clear();
        }
        
        // check felix health check
        if (checkEnabled) {
            if (healthCheckExecutor != null) {
                // https://github.com/apache/felix/tree/archived/healthcheck/webconsoleplugin

                HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
                options.setCombineTagsWithOr(checkCombineTagsWithOr);
                options.setForceInstantExecution(checkForceInstantExecution);
                if (isNotBlank(checkOverrideGlobalTimeoutStr))
                    try {
                        options.setOverrideGlobalTimeout(Integer.valueOf(checkOverrideGlobalTimeoutStr));
                    } catch (NumberFormatException nfe) {
                        // override not set in UI
                    }
                HealthCheckSelector selector = isNotBlank(checkTags) ? HealthCheckSelector.tags(checkTags.split(",")) : HealthCheckSelector.empty();
                Collection<HealthCheckExecutionResult> results = healthCheckExecutor.execute(selector, options);
                for (HealthCheckExecutionResult result : results) {
                    try {
                        String name = result.getHealthCheckMetadata().getName();
                        if (checkIgnore.contains(name)) continue;
                        Result status = result.getHealthCheckResult();
                        for (Entry entry : status) {
                            out.println(name + ": " + entry.getLogLevel() + " " + entry.getMessage());
                            Status s = entry.getStatus();
                            if (s != Status.OK)
                                healthy = false;
                        }
                    } catch (Throwable t) {
                        log.throwing("","",t);
                    }
                }
            } else {
                // direct ... legacy without HealthCheckExecutor service
                for (HealthCheck check : Osgi.getServices(HealthCheck.class, null)) {
                    try {
                        String name = check.toString();
                        int pos = name.indexOf('@');
                        if (pos > 0) name = name.substring(0,pos);
                        if (checkIgnore.contains(name)) continue;
                        Result status = check.execute();
                        for (Entry entry : status) {
                            out.println(name + ": " + entry.getLogLevel() + " " + entry.getMessage());
                            Status s = entry.getStatus();
                            if (s != Status.OK)
                                healthy = false;
                        }
                    } catch (Throwable t) {
                        log.throwing("","",t);
                    }
                }
            }
        }
        
        if (!healthy) {
            res.setStatus(501);
            out.println("status: error");
        } else {
            res.setStatus(200);
            out.println("status: ok");
        }
        
        res.setContentType("text/plain");

        out.flush();
        out.close();
        String content = new String(baos.toByteArray());
        res.getWriter().print(content);
        res.getWriter().flush();
        res.getWriter().close();
        if (!healthy) {
            log.severe("Ready check failed:\n" + content);
        }
    }

    private boolean isNotBlank(String str) {
        return str != null && str.length() > 0;
    }

    private static final class LogServiceTracker extends ServiceTracker<LogService, LogService> {

        private static final String SSHD_LOGGER = "org.apache.sshd";

        private final PaxAppender appender;

        private String sshdLoggerLevel;

        private LogServiceTracker(
                BundleContext context,
                Class<LogService> clazz,
                ServiceTrackerCustomizer<LogService, LogService> customizer,
                PaxAppender appender) {
            super(context, clazz, customizer);
            this.appender = appender;
        }

        @Override
        public LogService addingService(ServiceReference<LogService> reference) {
            LogService service = super.addingService(reference);
            sshdLoggerLevel = service.getLevel(SSHD_LOGGER).get(SSHD_LOGGER);
            service.setLevel(SSHD_LOGGER, "ERROR");
            service.addAppender(appender);
            return service;
        }

        @Override
        public void removedService(ServiceReference<LogService> reference, LogService service) {
            if (sshdLoggerLevel != null) {
                service.setLevel(SSHD_LOGGER, sshdLoggerLevel);
            }
            service.removeAppender(appender);
            // stopTail();
        }
    }

    private void printEvent(PaxLoggingEvent event) {
        // scan log
        try {
            if (event != null) {
                int sl = event.getLevel().getSyslogEquivalent();
                if (sl > logLevel) return;
                String msg = event.getMessage();
                for (Pattern pattern : logPatterns) {
                    if (pattern.matcher(msg).matches())
                        logFindings.add(pattern.pattern());
                }
            }
        } catch (NoClassDefFoundError e) {
            // KARAF-3350: Ignore NoClassDefFoundError exceptions
            // Those exceptions may happen if the underlying pax-logging service
            // bundle has been refreshed somehow.
        }
    }

    protected static int getMinLevel(String levelSt) {
        int minLevel = Integer.MAX_VALUE;
        if (levelSt != null) {
            switch (levelSt.toLowerCase()) {
                case "debug":
                    minLevel = DEBUG_INT;
                    break;
                case "info":
                    minLevel = INFO_INT;
                    break;
                case "warn":
                    minLevel = WARN_INT;
                    break;
                case "error":
                    minLevel = ERROR_INT;
                    break;
                case "all":
                    minLevel = ALL_INT;
                    break;
            }
        }
        return minLevel;
    }

}
