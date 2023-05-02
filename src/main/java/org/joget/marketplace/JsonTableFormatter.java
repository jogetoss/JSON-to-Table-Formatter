package org.joget.marketplace;

import java.util.HashMap;
import java.util.Map;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;

import org.joget.apps.app.service.AppPluginUtil;

public class JSONTableFormatter extends DataListColumnFormatDefault {
    private static final String MESSAGE_PATH = "messages/JSONToTableFormatter";
    
    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        Object[] parameters = null;
        if (getProperty("parameters") instanceof Object[]){
            parameters = (Object[]) getProperty("parameters");
        }
        
        HashMap<String, Map> mappings = new HashMap<String, Map>();
        for (Object obj : parameters) {
            Map mapping = (HashMap) obj;
            String jsonField = (String) mapping.get("jsonField");
            mappings.put(jsonField, mapping );
        }

                
        String colVal = (String) value;
        if (colVal != null && !colVal.isEmpty()) {
            value = HtmlTable.fromJson(colVal, mappings);
        }
        return (String) value;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("datalist.JSONToTableFormatter.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        return "7.0.0";
    }

    @Override
    public String getDescription() {
        return AppPluginUtil.getMessage("datalist.JSONToTableFormatter.desc", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getLabel() {
        return AppPluginUtil.getMessage("datalist.JSONToTableFormatter.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/JSONToTableFormatter.json", null, true, MESSAGE_PATH);
    }

}
