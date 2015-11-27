package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import java.io.File;

/**
 * Created by victor on 27/11/15.
 */
public class TrenUrbanoAppLayer extends OsmDataLayer {
    /**
     * Construct a new {@code OsmDataLayer}.
     *
     * @param data           OSM data
     * @param name           Layer name
     * @param associatedFile Associated .osm file (can be null)
     */
    public TrenUrbanoAppLayer(DataSet data, String name, File associatedFile) {
        super(data, name, associatedFile);
        setUploadDiscouraged(true);
    }

    @Override
    public void onPostSaveToFile() {
        setRequiresSaveToFile(false);
    }

    @Override
    public boolean requiresUploadToServer() {
        return false;
    }
}
