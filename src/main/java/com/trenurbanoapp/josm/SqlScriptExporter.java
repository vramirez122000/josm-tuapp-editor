package com.trenurbanoapp.josm;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileExporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

import javax.swing.*;
import java.io.File;
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
        try {
            SqlScriptWriter.write(data, file);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(MainApplication.getMainFrame(), tr(e.getMessage()), tr("Error saving file"), JOptionPane.ERROR_MESSAGE);
        }
        ((OsmDataLayer) layer).onPostSaveToFile();

    }

}
