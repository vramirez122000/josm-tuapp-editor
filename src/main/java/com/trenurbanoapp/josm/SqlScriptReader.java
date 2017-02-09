package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.*;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptReader {

    public static final int WGS84 = 4326;

    public static void read(final DataSet data, File file) throws IOException, SQLException {

        File stopsFile = file.getParentFile().toPath().resolve("../stops.sql").toFile();
        if (stopsFile.exists()) {
            readStops(data, stopsFile);
        }

        forEachLine(file, new LineCallback() {
            @Override
            public void doWithLine(String line) throws IOException, SQLException {
                readSubroute(data, line);
                readSubrouteStopRelation(data, line);
            }

        });
    }


    private static Pattern subroutePattern = Pattern.compile("ref\\.subroute\\s+", Pattern.CASE_INSENSITIVE);
    private static Pattern lineStringPattern = Pattern.compile("ST_GeomFromEWKT\\s*\\(\\s*'(SRID=4326;LINESTRING[^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern routeIdPattern = Pattern.compile("route\\s*=\\s*'([^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern directionPattern = Pattern.compile("direction\\s*=\\s*(?:NULL|'([^']*))", Pattern.CASE_INSENSITIVE);

    private static void readSubroute(DataSet data, String line) throws SQLException {
        Matcher m = subroutePattern.matcher(line);
        if (!m.find()) {
            return;
        }

        m = lineStringPattern.matcher(line);
        if(!m.find()) {
            throw new IllegalArgumentException("Linestring not found in: " + line);
        }
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

    private static Pattern relationPattern = Pattern.compile("ref\\.subroute_stop");
    private static Pattern relationValuesPattern = Pattern.compile("\\((\\s*[0-9]+\\s*,[^\\)]+)\\)");
    private static void readSubrouteStopRelation(DataSet data, String line) throws SQLException {
        Matcher m = relationPattern.matcher(line);
        if (!m.find()) {
            return;
        }

        Map<String, String> keys = new HashMap<>();
        m = routeIdPattern.matcher(line);
        if (m.find()) {
            keys.put("route", m.group(1));
        } else {
            return;
        }

        m = directionPattern.matcher(line);
        if (m.find()) {
            keys.put("direction", m.group(1));
            keys.put("name", m.group(1));
        } else {
            keys.put("direction", "");
            keys.put("name", "");
        }

        Relation rel = new Relation();
        m = relationValuesPattern.matcher(line);
        while(m.find()) {
            String[] valArray = m.group(1).split(",");
            final long stopGid = Long.parseLong(valArray[0].trim());
            Integer stopOrder = Integer.parseInt(valArray[1].trim());
            Node node = Utils.find(data.getNodes(), object -> {
                if(!object.getKeys().containsKey("gid")) {
                    return false;
                }
                final String gid = object.getKeys().get("gid");
                return Long.parseLong(gid) == stopGid;
            });
            RelationMember member = new RelationMember(String.valueOf(stopOrder), node);
            rel.addMember(stopOrder, member);
        }

        rel.setKeys(keys);
        data.addPrimitive(rel);
    }

    private static Pattern pointPattern = Pattern.compile("ST_GeomFromEWKT\\s*\\(\\s*'(SRID=4326;POINT[^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern stopGidPattern = Pattern.compile("([0-9]+)\\s+as\\s+gid", Pattern.CASE_INSENSITIVE);
    private static Pattern routesPattern = Pattern.compile("(?:NULL|'([^']*)')\\s+as\\s+routes", Pattern.CASE_INSENSITIVE);
    private static Pattern amaIdPattern = Pattern.compile("(?:NULL|([0-9]+))\\s+as\\s+ama_id", Pattern.CASE_INSENSITIVE);
    private static Pattern descriptioPattern = Pattern.compile("(?:NULL|'((?:[^']|'')*)')\\s+as\\s+descriptio", Pattern.CASE_INSENSITIVE);

    public static void readStops(final DataSet data, File file) {

        forEachLine(file, new LineCallback() {
            Set<Long> ids = new HashSet<>();

            @Override
            public void doWithLine(String line) throws IOException, SQLException {
                Matcher m = pointPattern.matcher(line);
                if (!m.find()) {
                    return;
                }

                String ewkt = m.group(1);
                Point p = new Point(ewkt);
                Node n = new Node(new LatLon(p.getY(), p.getX()));

                m = stopGidPattern.matcher(line);
                if (!m.find()) {
                    return;
                }
                long stopGid = Long.parseLong(m.group(1));
                if(ids.contains(stopGid)) {
                    throw new IllegalArgumentException("Non unique gid values");
                } else {
                    ids.add(stopGid);
                }

                //set the OSM ID in order to signal that data has been saved previously
                n.setOsmId(stopGid, 1);

                HashMap<String, String> keys = new HashMap<>();
                keys.put("highway", "bus_stop");
                keys.put("gid", String.valueOf(stopGid));

                m = routesPattern.matcher(line);
                if (m.find()) {
                    keys.put("routes", Utils.firstNonNull(m.group(1), ""));
                }

                m = descriptioPattern.matcher(line);
                if (m.find()) {
                    keys.put("name", Utils.firstNonNull(m.group(1), "").replaceAll("''", "'"));
                }

                m = amaIdPattern.matcher(line);
                if (m.find()) {
                    keys.put("ama_id", Utils.firstNonNull(m.group(1), ""));
                }

                n.setKeys(keys);
                data.addPrimitive(n);
            }
        });
    }


    interface LineCallback {
        void doWithLine(String line) throws IOException, SQLException;
    }

    public static void forEachLine(File file, LineCallback callback) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName("UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                callback.doWithLine(line);
            }

        } catch (IOException | SQLException ioe) {
            throw new IllegalArgumentException("Could not read file", ioe);
        }
    }
}
