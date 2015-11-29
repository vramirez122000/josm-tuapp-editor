package com.trenurbanoapp.josm;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.FileExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptExporter extends FileExporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter("sql", "sql", tr("Tren Urbano App SQL Script") + " (*.sql)");

    /**
     * Constructs a new {@code FileExporter}.
     */
    public SqlScriptExporter() {
        super(FILE_FILTER);
    }
    @Override
    public boolean acceptFile(File pathname, Layer layer) {
        return layer instanceof OsmDataLayer && super.acceptFile(pathname, layer);
    }

    @Override
    public void exportData(File file, Layer layer) throws IOException {
        if (!(layer instanceof OsmDataLayer)){
            throw new IllegalArgumentException("Data layer not supported: " + layer.getClass());
        }

        DataSet data = ((OsmDataLayer) layer).data;
        SqlScriptWriter.write(data, file);
        ((OsmDataLayer) layer).onPostSaveToFile();

    }

}
