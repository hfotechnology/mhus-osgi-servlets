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

    public static boolean checkBundles(ComponentContext ctx, ConfigTemplate config, PrintWriter out) {
        boolean healthy = true;
        for (Bundle bundle : ctx.getBundleContext().getBundles()) {
            if (bundle.getState() != Bundle.ACTIVE) {
                if (config.bundlesIgnore.contains(bundle.getSymbolicName()))
                    continue;
                if (out != null)
                    out.println("Bundle: " + bundle.getSymbolicName());
                else
                    return false;
                healthy = false;
            }
        }
        return healthy;
    }

    public static boolean checkServices(HealthCheckExecutor healthCheckExecutor, ConfigTemplate config, PrintWriter out, Logger log, Status ... alertStatus ) {
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
                options.setOverrideGlobalTimeout(Integer.valueOf(config.checkOverrideGlobalTimeoutStr));
            } catch (NumberFormatException nfe) {
                // override not set in UI
            }
        HealthCheckSelector selector = isNotBlank(config.checkTags) ? HealthCheckSelector.tags(config.checkTags.split(",")) : HealthCheckSelector.empty();
        Collection<HealthCheckExecutionResult> results = healthCheckExecutor.execute(selector, options);
        for (HealthCheckExecutionResult result : results) {
            try {
                String name = result.getHealthCheckMetadata().getName();
                if (config.checkIgnore.contains(name)) continue;
                Result status = result.getHealthCheckResult();
                for (Entry entry : status) {
                    out.println(name + ": " + entry.getLogLevel() + " " + entry.getMessage());
                    Status s = entry.getStatus();
                    for (Status alert : alertStatus)
                        if (s == alert)
                            healthy = false;
                }
            } catch (Throwable t) {
                log.throwing("","",t);
            }
        }
        
        return healthy;
    }

    private static boolean isNotBlank(String str) {
        return str != null && str.length() > 0;
    }

}
