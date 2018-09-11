package com.trenurbanoapp.josm;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.importexport.FileImporter;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

import javax.swing.*;
import java.io.File;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Created by victor on 27/11/15.
 */
public class SqlScriptImporter extends FileImporter {


    /**
     * Constructs a new {@code FileImporter} with the given extension file filter.
     */
    public SqlScriptImporter() {
        super(SqlScriptExporter.FILE_FILTER);
    }

    @Override
    public void importData(final File file, final ProgressMonitor progressMonitor) {

        progressMonitor.beginTask(tr("Loading Tren Urbano App SQL file..."));
        progressMonitor.setTicksCount(2);
        Logging.info("Parsing SQL: " + file.getAbsolutePath());

        DataSet data = new DataSet();
        try {
            SqlScriptReader.read(data, file);
            progressMonitor.worked(1);


            TrenUrbanoAppLayer layer = new TrenUrbanoAppLayer(data, "TUAPP: " + file.getName(), file);
            MainApplication.getLayerManager().addLayer(layer);
            
            System.out.println("Added layer.");

        } catch (Exception e) {

            Logging.error("Error while reading sql file!");
            Logging.error(e);
            GuiHelper.runInEDT(() -> {
                JOptionPane.showMessageDialog(null,
                        tr("Error loading sql file {0}", file.getAbsolutePath()),
                        tr("Error"),
                        JOptionPane.WARNING_MESSAGE
                ) ;
            });

        } finally {
            progressMonitor.finishTask();
        }

    }


}
