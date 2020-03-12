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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
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
import org.osgi.service.component.annotations.Deactivate;

@Component(
        service = Servlet.class,
        property = "alias=/dump/*",
        name = "DumpServlet",
        servicefactory = true)
public class DumpServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private Properties props;
    private static Logger log = Logger.getLogger(DumpServlet.class.getCanonicalName());

    @Activate
    public void activate(ComponentContext ctx) {
        props = new Properties();
        File f = new File("etc/dumpservlet.properties");
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
    }

    @Deactivate
    public void deactivate(ComponentContext ctx) {}

    public DumpServlet() {}

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String path = req.getPathInfo();
        
        boolean done = false;
        for (Object keyO : props.keySet()) {
            String key = keyO.toString();
            if (key.startsWith("_")) continue;
            if (path.matches(key)) {
                writeTo(req,props.getProperty(key));
                done = true;
            }
        }
        if (!done)
            log.fine("Request not logged: " + path);
        res.setStatus(200);
    }

    private synchronized void writeTo(HttpServletRequest req, String file) {
        try {
            if (file.equals("log")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream out = new PrintStream(baos);
                writeTo(req, out);
                out.close();
                log.info(new String(baos.toByteArray()));
            } else
            if (file.equals("-")) {
                writeTo(req, System.out);
            } else {
                FileOutputStream fos = new FileOutputStream(file, true);
                PrintStream out = new PrintStream(fos);
                writeTo(req, out);
                out.close();
            }
        } catch (Throwable t) {}
    }

    private void writeTo(HttpServletRequest req, PrintStream out) throws IOException, InterruptedException {
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
            if (!done)
                out.println(name + ":");
        }
        
        out.println();
        ServletInputStream is = req.getInputStream();
        byte[] buffer = new byte[1024];
        while (true) {
            int len = is.read(buffer);
            if (len < 0) break;
            if (len == 0)
                Thread.sleep(200);
            else
                out.write(buffer, 0, len);
        }
        out.flush();
    }
}
