package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.*;
import java.util.function.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    private static void writeSubroutes(DataSet dataSet, PrintWriter out) {

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
                points[i] = new Point(round(latLon.lon(), 6), round(latLon.lat(), 6));
            }

            LineString line = new LineString(points);
            line.setSrid(WGS84);
            String route = way.getKeys().get("route");
            String direction = way.getKeys().get("direction");

            out.printf("delete from ref.subroute where route = %s and direction = %s;", quoted(route), nullOrQuoted(direction));
            out.printf("with line_tmp as (select ST_Transform(ST_GeomFromEWKT(%s), 32161) geom) ", quoted(line.toString()));
            out.printf("insert into ref.subroute(route, direction, geom) select %s,%s, ST_AddMeasure(line_tmp.geom, 0, ST_Length(line_tmp.geom)) from line_tmp;",
                    quoted(route), nullOrQuoted(direction));
            out.println();
        }
        out.flush();
    }


    private static final Predicate<Node> STOP_PREDICATE = node ->
            !node.isDeleted() && node.hasTag("highway", "bus_stop");

    private static void writeStops(DataSet dataSet, File file) throws IOException{
        List<Node> stops = dataSet.getNodes().stream()
                .filter(STOP_PREDICATE)
                .collect(Collectors.toList());
        if (stops.isEmpty()) {
            return;
        }

        File stopsFile = new File(file.getParentFile().toPath().resolve("../stops.sql").toUri());
        if(!stopsFile.canWrite()) {
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(stopsFile))) {
            for (Node n : stops) {
                LatLon latLon = n.getCoor();
                Point p = new Point(round(latLon.lon(), 6), round(latLon.lat(), 6));
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
                out.print("insert into ref.subroute_stop (stop, stop_order, route, direction) values ");
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

        return String.format("'%s'", s.replaceAll("'(?!')", "''"));
    }
    
    private static String quoted(String s) {
        String escaped = Utils.firstNonNull(s, "").replaceAll("'(?!')", "''");
        return String.format("'%s'", escaped);
    }

    private static double round(double d, int places) {
        return new BigDecimal(d).setScale(places, RoundingMode.HALF_EVEN).doubleValue();
    }

}
