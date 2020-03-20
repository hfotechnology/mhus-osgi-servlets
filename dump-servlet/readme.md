## Overview

The dump servlet is used to dump requests into files for debugging reasons.

## Installation

```
install -s mvn:de.mhus.osgi/dump-servlet/7.0.0
```

## Configure

The configuration is done by OSGi configurations.

The config file is 'de.mhus.osgi.dumpservlet.DumpServlet.cfg'. You need to create the file.

```
contentType = [ \
  "", \
  ]
pathes = [ \
  "", \
  ]
payload = [ \
  "", \
  ]
status = [ \
  "", \
  ]
```
contentType:
insert path='file name' entries, e.g. test=data/log/test.log (pathes)

pathes:
Status return code for entries, e.g. test=404 (status)

payload:
Content Type for entries, e.g. test=application/json (contentType)

status:
Return payload for entries, e.g. test={"key":"value"} (payload)

