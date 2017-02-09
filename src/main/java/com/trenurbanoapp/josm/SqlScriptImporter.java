package com.trenurbanoapp.josm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.FileImporter;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptImporter extends FileImporter {

    private TrenUrbanoAppLayer layer;

    /**
     * Constructs a new {@code FileImporter} with the given extension file filter.
     */
    public SqlScriptImporter() {
        super(SqlScriptExporter.FILE_FILTER);
    }

    @Override
    public void importData(final File file, final ProgressMonitor progressMonitor) {

        DataSet data = new DataSet();
        try {
            SqlScriptReader.read(data, file);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(Main.parent, e, "Data Inconsistency Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException | SQLException e) {
            throw new IllegalArgumentException("could not parse sql script", e);
        }
        this.layer = new TrenUrbanoAppLayer(data, "TUAPP: " + file.getName(), file);

        GuiHelper.runInEDT(() -> {
            Main.main.addLayer(SqlScriptImporter.this.layer, (ProjectionBounds) null);
            System.out.println("Added layer.");
        });
    }


}
