package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptWriter {

    public static final int WGS84 = 4326;
    public static final Predicate<Node> STOP_PREDICATE = new Predicate<Node>() {
        @Override
        public boolean evaluate(Node object) {
            return object.hasTag("highway", "bus_stop");
        }
    };

    public static void write(DataSet dataSet, final File file) throws IOException {

        writeStops(dataSet, file);

        try(PrintWriter out = new PrintWriter(new FileWriter(file))) {
            Collection<Way> ways = dataSet.getSelectedWays();
            if (ways.isEmpty()) {
                ways = dataSet.getWays();
            }

            for (Way way : ways) {

                if (way.getNodes().isEmpty()) {
                    continue;
                }

                Point[] points = new Point[way.getNodes().size()];
                List<Node> nodes = way.getNodes();
                for (int i = 0, nodesSize = nodes.size(); i < nodesSize; i++) {
                    LatLon latLon = nodes.get(i).getCoor();
                    points[i] = new Point(latLon.lon(), latLon.lat());
                }

                LineString line = new LineString(points);
                line.setSrid(WGS84);
                String route = way.getKeys().get("route");
                String direction = way.getKeys().get("direction");

                out.printf("delete from ref.subroute_new where route = '%s' and direction = '%s';", route, direction);
                out.printf("with line_tmp as (select ST_Transform(ST_GeomFromEWKT('%s'), 32161) geom) ", line);
                out.printf("insert into ref.subroute_new(route, direction, geom) select '%s','%s', ST_AddMeasure(line_tmp.geom, 0, ST_Length(line_tmp.geom)) from line_tmp;\n",
                        route, direction);
                out.flush();
            }
        }
    }

    private static void writeStops(DataSet dataSet, File file) throws IOException{
        if(Utils.exists(dataSet.getNodes(), STOP_PREDICATE)) {
            File stopsFile = new File(file.getParentFile().toPath().resolve("../stops.sql").toUri());
            if(!stopsFile.canWrite()) {
                return;
            }

            try (PrintWriter stopsWriter = new PrintWriter(new FileWriter(stopsFile))) {
                for (Node n : Utils.filter(dataSet.getNodes(), STOP_PREDICATE)) {
                    LatLon latLon = n.getCoor();
                    Point p = new Point(latLon.lon(), latLon.lat());
                    p.setSrid(WGS84);
                    String gid = n.getKeys().get("gid");
                    if(gid == null) {
                        gid = String.valueOf(n.getId());
                    }
                    String routes = n.getKeys().get("routes");
                    String descriptio = n.getKeys().get("descriptio");
                    String amaId = n.getKeys().get("ama_id");

                    stopsWriter.printf("delete from ref.stop where gid = %s;", gid);
                    stopsWriter.print(" insert into ref.stop (gid, routes, descriptio, ama_id, geom) ");
                    stopsWriter.printf("select %s as gid, %s as routes, %s as descriptio, %s as ama_id, ST_GeomFromEWKT('%s');\n",
                            gid, dbStr(routes), dbStr(descriptio), dbStr(amaId), p);
                }
                stopsWriter.flush();
            }
        }
    }

    private static String dbStr(String s) {
        if(s == null || s.length() == 0) {
            return "NULL";
        }

        return String.format("'%s'", s.replaceAll("'", "''"));
    }
}
