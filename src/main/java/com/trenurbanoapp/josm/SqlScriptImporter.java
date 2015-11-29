package com.trenurbanoapp.josm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.FileImporter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
            SqlScriptReader.readSubroutes(data, file);
        } catch (IOException | SQLException e) {
            throw new IllegalArgumentException("could not parse sql script", e);
        }
        this.layer = new TrenUrbanoAppLayer(data, "TUAPP: " + file.getName(), file);

        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                Main.main.addLayer(SqlScriptImporter.this.layer);
                System.out.println("Added layer.");
            }
        });
    }


}
