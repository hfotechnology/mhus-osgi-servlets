/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.logging.Logger;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

public class HealthCheckUtil {

    public static final int ERROR_INT = 3;
    public static final int WARN_INT = 4;
    public static final int INFO_INT = 6;
    public static final int DEBUG_INT = 7;
    public static final int ALL_INT = 100;

    public enum LOG_LEVEL {
        ERROR {
            @Override
            public int toInt() {
                return ERROR_INT;
            }
        },
        WARN {
            @Override
            public int toInt() {
                return WARN_INT;
            }
        },
        INFO {
            @Override
            public int toInt() {
                return INFO_INT;
            }
        },
        DEBUG {
            @Override
            public int toInt() {
                return DEBUG_INT;
            }
        },
        ALL {
            @Override
            public int toInt() {
                return ALL_INT;
            }
        };

        public int toInt() {
            return 0;
        }
    }

    public static boolean checkBundles(ComponentContext ctx, ConfigValues config, PrintWriter out) {
        boolean healthy = true;
        for (Bundle bundle : ctx.getBundleContext().getBundles()) {
            if (bundle.getState() != Bundle.ACTIVE) {
                if (config.bundlesIgnore.contains(bundle.getSymbolicName())) continue;
                if (out != null) out.println("Bundle: " + bundle.getSymbolicName());
                else return false;
                healthy = false;
            }
        }
        return healthy;
    }

    public static boolean checkOSGiHealthServices(
            HealthCheckExecutor healthCheckExecutor,
            ConfigValues config,
            PrintWriter out,
            Logger log,
            Status... alertStatus) {
        if (healthCheckExecutor == null) {
            out.println("Error: healthCheckExecutor not present");
            return false;
        }

        boolean healthy = true;
        HealthCheckExecutionOptions options = new HealthCheckExecutionOptions();
        options.setCombineTagsWithOr(config.checkCombineTagsWithOr);
        options.setForceInstantExecution(config.checkForceInstantExecution);
        if (isNotBlank(config.checkOverrideGlobalTimeoutStr))
            try {
                options.setOverrideGlobalTimeout(
                        Integer.valueOf(config.checkOverrideGlobalTimeoutStr));
            } catch (NumberFormatException nfe) {
                // override not set in UI
            }
        HealthCheckSelector selector =
                isNotBlank(config.checkTags)
                        ? HealthCheckSelector.tags(config.checkTags.split(","))
                        : HealthCheckSelector.empty();
        Collection<HealthCheckExecutionResult> results =
                healthCheckExecutor.execute(selector, options);
        for (HealthCheckExecutionResult result : results) {
            try {
                String name = result.getHealthCheckMetadata().getName();
                if (config.checkIgnore.contains(name)) continue;
                Result status = result.getHealthCheckResult();
                for (Entry entry : status) {
                    out.println(
                            "OSGiHealth: "
                                    + name
                                    + "="
                                    + entry.getLogLevel()
                                    + ","
                                    + entry.getMessage());
                    Status s = entry.getStatus();
                    for (Status alert : alertStatus) if (s == alert) healthy = false;
                }
            } catch (Throwable t) {
                log.throwing("", "", t);
            }
        }

        return healthy;
    }

    private static boolean isNotBlank(String str) {
        return str != null && str.length() > 0;
    }
}
