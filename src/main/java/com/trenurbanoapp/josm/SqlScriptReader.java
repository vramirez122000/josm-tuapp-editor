package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Utils;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptReader {

    public static final int WGS84 = 4326;
    private static Pattern lineStringPattern = Pattern.compile("ST_GeomFromEWKT\\s*\\(\\s*'(SRID=4326;LINESTRING[^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern routeIdPattern = Pattern.compile("route\\s*=\\s*'([^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern directionPattern = Pattern.compile("direction\\s*=\\s*(?:NULL|'([^']*))", Pattern.CASE_INSENSITIVE);

    public static void readSubroutes(final DataSet data, File file) throws IOException, SQLException {

        File stopsFile = file.getParentFile().toPath().resolve("../stops.sql").toFile();
        if(stopsFile.exists()) {
            readStops(data, stopsFile);
        }


        forEachLine(file, new LineCallback() {
            @Override
            public void doWithLine(String line) throws IOException, SQLException {
                Matcher m = lineStringPattern.matcher(line);
                if (m.find()) {
                    String ewkt = m.group(1);
                    LineString lineString = new LineString(ewkt);
                    Way w = new Way();
                    for (int i = 0; i < lineString.getPoints().length; i++) {
                        Point p = lineString.getPoints()[i];
                        Node n = new Node(new LatLon(p.getY(), p.getX()));
                        data.addPrimitive(n);
                        w.addNode(n);
                    }

                    Map<String, String> keys = new HashMap<>();
                    keys.put("oneway", "yes");
                    keys.put("highway", "primary");


                    m = routeIdPattern.matcher(line);
                    if (m.find()) {
                        keys.put("route", m.group(1));
                    }

                    m = directionPattern.matcher(line);
                    if (m.find()) {
                        keys.put("direction", m.group(1));
                    }
                    w.setKeys(keys);
                    data.addPrimitive(w);
                }
            }
        });
    }

    private static Pattern pointPattern = Pattern.compile("ST_GeomFromEWKT\\s*\\(\\s*'(SRID=4326;POINT[^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern stopGidPattern = Pattern.compile("([0-9]+)\\s+as\\s+gid", Pattern.CASE_INSENSITIVE);
    private static Pattern routesPattern = Pattern.compile("(?:NULL|'([^']*)')\\s+as\\s+routes", Pattern.CASE_INSENSITIVE);
    private static Pattern amaIdPattern = Pattern.compile("(?:NULL|([0-9]+))\\s+as\\s+ama_id", Pattern.CASE_INSENSITIVE);
    private static Pattern descriptioPattern = Pattern.compile("(?:NULL|'((?:[^']|'')*)')\\s+as\\s+descriptio", Pattern.CASE_INSENSITIVE);

    public static void readStops(final DataSet data, File file) {

        forEachLine(file, new LineCallback() {
            @Override
            public void doWithLine(String line) throws IOException, SQLException {
                Matcher m = pointPattern.matcher(line);
                if (m.find()) {
                    String ewkt = m.group(1);
                    Point p = new Point(ewkt);
                    Node n = new Node(new LatLon(p.getY(), p.getX()));

                    m = stopGidPattern.matcher(line);
                    if (m.find()) {
                        n.setOsmId(Long.parseLong(m.group(1)), 1);
                    }

                    HashMap<String, String> keys = new HashMap<>();
                    keys.put("highway", "bus_stop");

                    m = routesPattern.matcher(line);
                    if (m.find()) {
                        keys.put("routes", Utils.firstNonNull(m.group(1), ""));
                    }

                    m = descriptioPattern.matcher(line);
                    if (m.find()) {
                        keys.put("descriptio", Utils.firstNonNull(m.group(1), ""));
                    }

                    m = amaIdPattern.matcher(line);
                    if (m.find()) {
                        keys.put("ama_id", Utils.firstNonNull(m.group(1), ""));
                    }

                    n.setKeys(keys);
                    data.addPrimitive(n);


                }
            }
        });
    }


    interface LineCallback {
        void doWithLine(String line) throws IOException, SQLException;
    }

    public static void forEachLine(File file, LineCallback callback) {
        try(BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName("UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                callback.doWithLine(line);
            }

        } catch (IOException | SQLException ioe) {
            throw new IllegalArgumentException("Could not read file", ioe);
        }
    }
}
