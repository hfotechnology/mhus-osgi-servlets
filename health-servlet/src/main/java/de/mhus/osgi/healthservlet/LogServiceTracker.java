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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.karaf.log.core.LogService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class LogServiceTracker extends ServiceTracker<LogService, LogService> {

    private static final String SSHD_LOGGER = "org.apache.sshd";

    private final PaxAppender appender;

    private String sshdLoggerLevel;

    private ConfigValues config;

    Set<String> logFindings = Collections.synchronizedSet(new HashSet<>());

    public LogServiceTracker(
            BundleContext context,
            Class<LogService> clazz,
            ServiceTrackerCustomizer<LogService, LogService> customizer,
            ConfigValues config) {
        super(context, clazz, customizer);
        this.config = config;
        this.appender = event -> printEvent(event);
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

    private void printEvent(PaxLoggingEvent event) {
        // scan log
        try {
            if (event != null) {
                int sl = event.getLevel().getSyslogEquivalent();
                if (sl > config.logLevel) return;
                String msg = event.getMessage();
                for (Pattern pattern : config.logPatterns) {
                    if (pattern.matcher(msg).matches()) logFindings.add(pattern.pattern());
                }
            }
        } catch (NoClassDefFoundError e) {
            // KARAF-3350: Ignore NoClassDefFoundError exceptions
            // Those exceptions may happen if the underlying pax-logging service
            // bundle has been refreshed somehow.
        }
    }
}
