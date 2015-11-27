package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.postgis.LineString;
import org.postgis.Point;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptWriter {

    public static final int WGS84 = 4326;

    public static void write(DataSet dataSet, Writer out) throws IOException {
        for (Way way : dataSet.getWays()) {
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

            out.write("delete from ref.subroute_new where route = '" + route + "'" +
                    " and direction = '" + direction + "';" +
                    " with line_tmp as (select ST_Transform(ST_GeomFromEWKT('" + line.toString() + "'), 32161) geom)" +
                    " insert into ref.subroute_new(route, direction, geom) select " +
                    "'" + route + "'," +
                    "'" + direction + "'," +
                    " ST_AddMeasure(line_tmp.geom, 0, ST_Length(line_tmp.geom)) from line_tmp" +
                    ";\n");
        }
    }
}
