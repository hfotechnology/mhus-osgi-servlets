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

import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class Osgi {

    public static final String COMPONENT_NAME = "component.name";

    public static <T> T getService(Class<T> ifc, String filter) {
        List<T> list = getServices(ifc, filter);
        if (list.size() == 0) return null;
        return list.get(0);
    }

    public static <T> List<T> getServices(Class<T> ifc, String filter) {
        BundleContext context = FrameworkUtil.getBundle(ifc).getBundleContext();
        if (context == null) context = FrameworkUtil.getBundle(Osgi.class).getBundleContext();
        if (context == null) return null;
        LinkedList<T> out = new LinkedList<>();
        try {
            for (ServiceReference<T> ref : context.getServiceReferences(ifc, filter)) {
                T obj = context.getService(ref);
                out.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }


}
