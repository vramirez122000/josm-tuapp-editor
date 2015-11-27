package com.trenurbanoapp.josm;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
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
        String sqlScript;
        try {
            sqlScript = readFile(file, Charset.forName("UTF-8"));
        } catch (final IOException e) {
            throw new IllegalArgumentException("Could not read file into string", e);
        }
        DataSet data = new DataSet();
        try {
            SqlScriptReader.read(data, sqlScript);
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

    static String readFile(File file, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
        return new String(encoded, encoding);
    }
}
