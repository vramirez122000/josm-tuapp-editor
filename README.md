# josm-tuapp-sql

A plugin for editing data for Tren Urbano App subroutes, stops and the relation between these.

## Build, install and run JOSM

```
./gradlew runJosm
```

And Josm should pick up the plugin at startup.

To Activate it, Josm Preferences > Plugins > Search josm-tuapp-editor > Click the check box > Ok.

To save a file as Tren Urbano App SQL, select a layer, Save As -> Tren Urbano App SQL Script (*.sql)

To open a subroute `*.sql` file, File > Open, select the file

## Aerial Imagery

This plugin automatically installs an Imagery source called '2010 Puerto Rico USACE' for tracing

## Project Dependencies

Java 8 Development Kit

All other dependencies are automatically fetched from the Maven Central Repository:

* PostgreSQL JDBC Driver 
* PostGIS Client JDBC Wrapper