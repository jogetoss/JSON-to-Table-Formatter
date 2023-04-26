package org.joget.marketplace;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;

import java.util.HashMap;

public class JsonTableFormatter extends DataListColumnFormatDefault {

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        String jsonField = getPropertyString("jsonField");
        String label = getPropertyString("label");
        HashMap<String, String> columnLabel= new HashMap<String, String>();
        String[] keys = jsonField.split(";");
        String[] values = label.split(";");

        for(int x=0;x<keys.length;x++) {
            columnLabel.put(keys[x],values[x]);
        }

        String colVal = (String) value;
        if (colVal != null && !colVal.isEmpty()) {
            value = HtmlTable.fromJson(colVal,columnLabel);
        }
        return (String) value;
    }

    @Override
    public String getName() {
        return "Json to Table Formatter2";
    }

    @Override
    public String getVersion() {
        return "7.0.0";
    }

    @Override
    public String getDescription() {
        return "To format json format into table format";
    }

    @Override
    public String getLabel() {
        return "Json to Table Formatter2";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/JsonToTableFormatter.json", null, true, "message/JsonToTableFormatter");
    }

}
