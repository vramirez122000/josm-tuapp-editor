package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptReader {

    public static final int WGS84 = 4326;
    private static Pattern ewkt = Pattern.compile("ST_GeomFromEWKT\\s*\\(\\s*'([^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern route = Pattern.compile("route\\s*=\\s*'([^']*)", Pattern.CASE_INSENSITIVE);
    private static Pattern direction = Pattern.compile("direction\\s*=\\s*'([^']*)", Pattern.CASE_INSENSITIVE);

    public static void read(DataSet data, String script) throws IOException, SQLException {
        Matcher m = ewkt.matcher(script);
        List<Way> ways = new ArrayList<>();
        while (m.find()) {
            String ewkt = m.group(1);
            LineString line = new LineString(ewkt);
            Way w = new Way();
            for (int i = 0; i < line.getPoints().length; i++) {
                Point p = line.getPoints()[i];
                Node n = new Node(new LatLon(p.getY(), p.getX()));
                data.addPrimitive(n);
                w.addNode(n);
            }
            ways.add(w);
        }

        String[] routes = new String[ways.size()];
        m = route.matcher(script);
        int i = 0;
        while(m.find()) {
            routes[i++] = m.group(1);
        }

        String[] directions = new String[ways.size()];
        m = direction.matcher(script);
        int j = 0;
        while(m.find()) {
            directions[j++] = m.group(1);
        }

        int k = 0;
        for (Way way : ways) {
            Map<String, String> keys = new HashMap<>(ways.size());
            keys.put("route", routes[k]);
            keys.put("direction", directions[k]);
            way.setKeys(keys);
            data.addPrimitive(way);
            k++;
        }
    }
}
