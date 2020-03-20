## Overview

The servlet is use to redirect unused paths to usefully locations. The main usage is to redirect the root '/' to another servlet like '/ui'.

## Install
```
feature:install http-whiteboard
install -s mvn:de.mhus.osgi/root-servlet/7.0.0
```

## Configure

The configuration is done by OSGi configuration framework. Create the config file de.mhus.osgi.rootservlet.RootServlet.cfg or use the webconsole configuration UI.

You can define a list of regex and the target. The target can use groups of the regex pattern.

e.g.

```
redirects = [ \
  ".*=/ui", \
  ]
```

Redirects every path to /ui

```
redirects = [ \
  "(.*)=/ui?origin=$1", \
  ]
```

Redirects to /ui and remember the original path.

Define the error message and code if no path is found.

```
errorNr=404
errorMsg=
```
 
