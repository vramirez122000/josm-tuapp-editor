package com.trenurbanoapp.josm;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.imagery.ImageryLayerInfo;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Created by victor on 27/11/15.
 */
public class TrenUrbanoAppSqlPlugin extends Plugin {

    private final SqlScriptImporter sqlScriptImporter;
    private final SqlScriptExporter sqlScriptExporter;
    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public TrenUrbanoAppSqlPlugin(PluginInformation info) {
        super(info);
        sqlScriptImporter = new SqlScriptImporter();

        ExtensionFileFilter.addImporter(this.sqlScriptImporter);
        ExtensionFileFilter.updateAllFormatsImporter();

        sqlScriptExporter = new SqlScriptExporter();
        ExtensionFileFilter.addExporter(this.sqlScriptExporter);


        if(!ImageryLayerInfo.instance.getLayers().contains(PuertoRicoImagery.ArcgisOnline2010Usace)) {
            ImageryLayerInfo.addLayer(PuertoRicoImagery.ArcgisOnline2010Usace);
        }
    }
}
