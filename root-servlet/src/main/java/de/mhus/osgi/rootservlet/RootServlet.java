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
package de.mhus.osgi.rootservlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/*

default.redirect=

rule0=.*
rule0.redirect=

 */
@Component(
        service = Servlet.class,
        property = "alias=/*",
        servicefactory = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL
        )
@Designate(ocd = RootServlet.Config.class)
public class RootServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(RootServlet.class.getCanonicalName());
    private HashMap<Pattern, String> redirects = new HashMap<>();
    private String errorMsg;
    private int errorNr;

    @ObjectClassDefinition(name = "Health Check Servlet", description = "For Kubernetes")
    public @interface Config {
        @AttributeDefinition(name = "Redirect", description = "Redirect a path to another one, key is a regex, the value the target path, e.g. .*=/ui")
        String[] redirect() default {};
        String errorMsg() default "";
        int errorNr() default 404;
    }
    
    @Activate
    public void activate(ComponentContext ctx, Config c) {
        loadConfig(c);
    }

    @Modified
    public void modified(ComponentContext ctx, Config c) {
        loadConfig(c);
    }

    private void loadConfig(Config c) {
        redirects.clear();
        for (String entry : c.redirect()) {
            int pos = entry.indexOf('=');
            if (pos > 0) {
                String regex = entry.substring(0,pos);
                String target = entry.substring(pos+1);
                Pattern pattern = Pattern.compile(regex);
                redirects.put(pattern, target);
            }
        }
        errorMsg = c.errorMsg();
        errorNr = c.errorNr();
    }

    @Deactivate
    public void deactivate(ComponentContext ctx) {}

    public RootServlet() {}

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getPathInfo();

        if (path == null) path = "";

        for (Entry<Pattern, String> entry : redirects.entrySet()) {
            Matcher matcher = entry.getKey().matcher(path);
            if (matcher.matches()) {
                String target = matcher.replaceFirst(entry.getValue());
                res.sendRedirect(target);
                return;
            }
        }
        log.fine("No match for path: " + path);

        if (errorMsg.length() > 0)
            res.sendError(errorNr, errorMsg);
        else
            res.sendError(errorNr);
    }
}
