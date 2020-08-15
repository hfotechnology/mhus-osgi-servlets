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
package de.mhus.osgi.dumpservlet;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
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

@Component(
        service = Servlet.class,
        property = "alias=/dump/*",
        servicefactory = true,
        configurationPolicy = ConfigurationPolicy.OPTIONAL)
@Designate(ocd = DumpServlet.Config.class)
public class DumpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Map<String, Definition> pathes = new HashMap<>();
    private static Logger log = Logger.getLogger(DumpServlet.class.getCanonicalName());

    @ObjectClassDefinition(name = "Dump Servlet", description = "Dump requests into files")
    public @interface Config {
        @AttributeDefinition(
                name = "Path",
                description = "insert path='file name' entries, e.g. test=data/log/test.log")
        String[] pathes() default {};

        @AttributeDefinition(
                name = "Status",
                description = "Status return code for entries, e.g. test=404")
        String[] status() default {};

        @AttributeDefinition(
                name = "Content Type",
                description = "Content Type for entries, e.g. test=application/json")
        String[] contentType() default {};

        @AttributeDefinition(
                name = "Payload",
                description = "Return payload for entries, e.g. test={\"key\":\"value\"}")
        String[] payload() default {};
    }

    public static class Definition {
        String path;
        String file;
        int status = 200;
        String contentType = "application/json";
        String payload = null;
    }

    @Activate
    public void activate(ComponentContext ctx, Config c) {
        configure(c);
    }

    @Modified
    public void modified(ComponentContext ctx, Config c) {
        configure(c);
    }

    private void configure(Config c) {
        pathes.clear();
        for (String p : c.pathes()) {
            int pos = p.indexOf('=');
            if (pos > 0) {
                Definition def = new Definition();
                def.path = p.substring(0, pos);
                def.file = p.substring(pos + 1);

                String prefix = def.path + "=";
                for (String x : c.status())
                    if (x.startsWith(prefix))
                        def.status = Integer.parseInt(x.substring(prefix.length()));
                for (String x : c.contentType())
                    if (x.startsWith(prefix)) def.contentType = x.substring(prefix.length());
                for (String x : c.payload())
                    if (x.startsWith(prefix)) def.payload = x.substring(prefix.length());

                pathes.put(def.path, def);
            }
        }
    }

    @Deactivate
    public void deactivate(ComponentContext ctx) {}

    public DumpServlet() {}

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getPathInfo();

        Definition def = null;
        for (String key : pathes.keySet()) {
            if (key.startsWith("_")) continue;
            if (path.matches(key)) {
                def = pathes.get(key);
                writeTo(req, def.file);
                break;
            }
        }
        if (def == null) {
            log.fine("Request not logged: " + path);
            res.setStatus(404);
        } else {
            res.setStatus(def.status);
            res.setContentType(def.contentType);
            if (def.payload != null) res.getWriter().write(def.payload);
        }
    }

    private synchronized void writeTo(HttpServletRequest req, String file) {
        try {
            if (file.equals("log")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos);
                writeTo(req, out);
                out.close();
                log.info(new String(baos.toByteArray()));
            } else if (file.equals("-")) {
                writeTo(req, System.out);
            } else {
                FileOutputStream fos = new FileOutputStream(file, true);
                PrintStream out = new PrintStream(fos);
                writeTo(req, out);
                out.close();
            }
        } catch (Throwable t) {
        }
    }

    private void writeTo(HttpServletRequest req, PrintStream out)
            throws IOException, InterruptedException {
        out.println("----- Request " + new Date() + " from " + req.getRemoteAddr());
        out.println(req.getMethod() + " " + req.getPathInfo() + " " + req.getQueryString());
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            boolean done = false;
            Enumeration<String> headers = req.getHeaders(name);
            while (headers.hasMoreElements()) {
                String header = headers.nextElement();
                out.println(name + ": " + header);
                done = true;
            }
            if (!done) out.println(name + ":");
        }

        out.println();
        ServletInputStream is = req.getInputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int len = is.read(buffer);
            if (len < 0) break;
            if (len == 0) Thread.sleep(200);
            else out.write(buffer, 0, len);
        }
        out.flush();
    }
}
