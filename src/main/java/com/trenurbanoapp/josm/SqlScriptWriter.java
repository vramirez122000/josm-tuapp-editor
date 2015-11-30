package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptWriter {

    public static final int WGS84 = 4326;
    public static void write(DataSet dataSet, final File file) throws IOException {

        writeStops(dataSet, file);
        try(PrintWriter out = new PrintWriter(new FileWriter(file))) {
            writeSubroutes(dataSet, out);
            writeRelations(dataSet, out);
        }
    }

    private static void writeSubroutes(DataSet dataSet, PrintWriter out) throws IOException {

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

            out.printf("delete from ref.subroute_new where route = %s and direction = %s;", quoted(route), nullOrQuoted(direction));
            out.printf("with line_tmp as (select ST_Transform(ST_GeomFromEWKT(%s), 32161) geom) ", quoted(line.toString()));
            out.printf("insert into ref.subroute_new(route, direction, geom) select %s,%s, ST_AddMeasure(line_tmp.geom, 0, ST_Length(line_tmp.geom)) from line_tmp;",
                    quoted(route), nullOrQuoted(direction));
            out.println();
        }
        out.flush();
    }


    public static final Predicate<Node> STOP_PREDICATE = new Predicate<Node>() {
        @Override
        public boolean evaluate(Node object) {
            return !object.isDeleted() && object.hasTag("highway", "bus_stop");
        }
    };
    private static void writeStops(DataSet dataSet, File file) throws IOException{
        if (!Utils.exists(dataSet.getNodes(), STOP_PREDICATE)) {
            return;
        }

        File stopsFile = new File(file.getParentFile().toPath().resolve("../stops.sql").toUri());
        if(!stopsFile.canWrite()) {
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(stopsFile))) {
            for (Node n : Utils.filter(dataSet.getNodes(), STOP_PREDICATE)) {
                LatLon latLon = n.getCoor();
                Point p = new Point(latLon.lon(), latLon.lat());
                p.setSrid(WGS84);
                String gid = n.getKeys().get("gid");
                if(gid == null || gid.trim().length() == 0) {
                    throw new IllegalArgumentException("Please assign a unique 'gid' value to newly added stops");
                }
                String routes = n.getKeys().get("routes");
                String descriptio = n.getKeys().get("name");
                String amaId = n.getKeys().get("ama_id");

                out.printf("delete from ref.stop where gid = %s;", gid);
                out.print(" insert into ref.stop (gid, routes, descriptio, ama_id, geom) ");
                out.printf("select %s as gid, %s as routes, %s as descriptio, %s as ama_id, ST_GeomFromEWKT(%s);",
                        gid, nullOrQuoted(routes), nullOrQuoted(descriptio), nullOrQuoted(amaId), quoted(p.toString()));
                out.println();
            }
            out.flush();
        }
    }

    private static void writeRelations(DataSet dataSet, PrintWriter out) {
        if(dataSet.getRelations().isEmpty()) {
            return;
        }

        for (Relation relation : dataSet.getRelations()) {
            if(relation.getMembers().isEmpty()) {
                continue;
            }

            String route = relation.getKeys().get("route");
            if(route == null || route.length() == 0) {
                continue;
            }

            String direction = relation.getKeys().get("direction");

            List<RelationMember> members = relation.getMembers();
            if(members.isEmpty()) {
                return;
            } else {
                out.printf("delete from ref.subroute_stop where route = %s and direction = %s; ",
                        quoted(route),
                        nullOrQuoted(direction));
                out.print("insert into ref.subroute_stop (stop, stop_order, direction, stop) values ");
            }
            for (int i = 0; i < members.size(); i++) {
                if(i > 0) {
                    out.print(',');
                }

                RelationMember member = members.get(i);
                if (!OsmPrimitiveType.NODE.equals(member.getType())) {
                    continue;
                }

                long stopGid;
                long osmId = member.getUniqueId();
                if (osmId > 0) {
                    stopGid = osmId;
                } else {
                    stopGid = Long.parseLong(member.getNode().getKeys().get("gid"));
                }

                out.printf("(%s, %s, %s, %s)", stopGid, i, quoted(route), nullOrQuoted(direction));
            }
            out.println(";");
        }
        out.flush();
    }

    private static String nullOrQuoted(String s) {
        if(s == null || s.length() == 0) {
            return "NULL";
        }

        return String.format("'%s'", s.replaceAll("'", "''"));
    }
    
    private static String quoted(String s) {
        return String.format("'%s'", Utils.firstNonNull(s, "").replaceAll("'", "''"));
    }
}
