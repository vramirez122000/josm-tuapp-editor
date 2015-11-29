package com.trenurbanoapp.josm

import org.junit.Test
import org.openstreetmap.josm.Main
import org.openstreetmap.josm.data.Preferences
import org.openstreetmap.josm.data.osm.DataSet
import spock.lang.Specification

/**
 * Created by victor on 27/11/15.
 */
class SqlScriptReaderTests extends Specification {

    @Test
    def "test read"(String sql) {
        setup:
        Main.pref = new Preferences()
        DataSet data = new DataSet();
        SqlScriptReader.readSubroutes(data, sql)

        expect:
        data.getWays().size() > 0

        where:
        sql  | _
        "delete from ref.subroute_new where route = 'AX1' and direction = 'San Juan'; insert into ref.subroute_new(route, direction, geom) values ( 'AX1','San Juan', ST_AddMeasure(ST_Transform(ST_GeomFromEWKT('SRID=4326;LINESTRING(-66.11666293855 18.44370301523,-66.11688854895 18.44404735246,-66.11689401039 18.44451811416,-66.11273426685 18.46051577613,-66.11270429438 18.4609906272,-66.1130815 18.463019)'), 32161));\n" +
                "delete from ref.subroute_new where route = 'AX1' and direction = 'Cataño'; insert into ref.subroute_new(route, direction, geom) values ( 'AX1','Cataño', ST_AddMeasure(ST_Transform(ST_GeomFromEWKT('SRID=4326;LINESTRING(-66.1130815 18.463019,-66.11270429438 18.4609906272,-66.11273426685 18.46051577613,-66.11689401039 18.44451811416,-66.11688854895 18.44404735246,-66.11666293855 18.44370301523)'), 32161));".toString() | _
    }


}
