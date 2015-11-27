# josm-tuapp-sql

A plugin for editing data for Tren Urbano App

## Status

[![Build Status](https://travis-ci.org/matthieun/josm-geojson.svg?branch=master)](https://travis-ci.org/matthieun/josm-geojson)

## Build

Download josm
```
gradle downloadJosm
```
Build
```
gradle clean build
```


## Run

Install
```
gradle installPlugin
```

And Josm should pick up the plugin at startup.

To Activate it, Josm Preferences > Plugins > Search josm-tuapp-editor > Click the check box > Ok.

To save a file as Tren Urbano App SQL, select a layer, Save As -> Tren Urbano App SQL Script (*.sql)

To open a `*.sql` file, File > Open, select the file