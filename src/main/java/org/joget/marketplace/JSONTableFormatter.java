package org.joget.marketplace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.Collections;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.DataList;
import org.joget.apps.datalist.model.DataListColumn;
import org.joget.apps.datalist.model.DataListColumnFormatDefault;
import org.joget.apps.datalist.service.DataListService;
import org.displaytag.tags.TableTagParameters;

import org.joget.apps.app.service.AppPluginUtil;
import org.joget.commons.util.LogUtil;

public class JSONTableFormatter extends DataListColumnFormatDefault {
    private static final String MESSAGE_PATH = "messages/JSONToTableFormatter";

    //datalists that already received the audit trail CSS
    private static final Map<DataList, Boolean> STYLED = Collections.synchronizedMap(new WeakHashMap<DataList, Boolean>());

    @Override
    public String format(DataList dataList, DataListColumn column, Object row, Object value) {
        if ("auditTrail".equals(getPropertyString("displayMode"))) {
            //the value passed in is already HTML escaped, so read the raw JSON from the row
            Object raw = DataListService.evaluateColumnValueFromRow(row, column.getName());
            String json = (raw != null) ? raw.toString() : null;
            AuditTrailTable auditTable = new AuditTrailTable(getClassName(),
                    "true".equalsIgnoreCase(getPropertyString("showFieldId")),
                    "true".equalsIgnoreCase(getPropertyString("expandByDefault")));

            String result;
            if ("1".equals(dataList.getDataListParamString(TableTagParameters.PARAMETER_EXPORTING))) {
                result = auditTable.renderText(json);
            } else {
                String author = null;
                Object date = null;
                if ("true".equalsIgnoreCase(getPropertyString("showAuthor"))) {
                    Object name = DataListService.evaluateColumnValueFromRow(row, "createdByName");
                    if (name == null || name.toString().isEmpty()) {
                        name = DataListService.evaluateColumnValueFromRow(row, "createdBy");
                    }
                    author = (name != null) ? name.toString() : null;
                    //already formatted with the app's date format and the user's time zone
                    date = DataListService.evaluateColumnValueFromRow(row, "dateCreated");
                }
                result = auditTable.render(json, author, date, STYLED.put(dataList, Boolean.TRUE) == null);
            }
            if (result != null) {
                return result;
            }
        }

        String showUnmapped = getPropertyString("showUnmappedFields");
        String headerFontColor = getPropertyString("headerFontColor");
        String headerBgColor = getPropertyString("headerBgColor");
        String tableInlineStyle = getPropertyString("tableInlineStyle");
        String transpose = getPropertyString("transpose");

        ArrayList<String> tableConfig = new ArrayList<String>();

        tableConfig.add(showUnmapped);
        tableConfig.add(headerFontColor);
        tableConfig.add(headerBgColor);
        tableConfig.add(tableInlineStyle);
        tableConfig.add(transpose);

        Object[] parameters = null;
        if (getProperty("parameters") instanceof Object[]){
            parameters = (Object[]) getProperty("parameters");
        }
        
        LinkedHashMap<String, Map> mappings = new LinkedHashMap<String, Map>();
        for (Object obj : parameters) {
            Map mapping = (HashMap) obj;
            String jsonField = (String) mapping.get("jsonField");
            mappings.put(jsonField, mapping );
        }


        String colVal = (String) value;
        if (colVal != null && !colVal.isEmpty()) {
            value = HtmlTable.fromJson(colVal, mappings, tableConfig);
        }
        return (String) value;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("datalist.JSONToTableFormatter.name", getClassName(), MESSAGE_PATH);
    }

    @Override
    public String getVersion() {
        final Properties projectProp = new Properties();
        try {
            projectProp.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException ex) {
            LogUtil.error(getClass().getName(), ex, "Unable to get project version from project properties...");
        }
        return projectProp.getProperty("version");
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
