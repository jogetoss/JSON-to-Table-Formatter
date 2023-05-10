package org.joget.marketplace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class HtmlTable {
    private HtmlTable() {
    }

    public static String fromJson(String json, LinkedHashMap<String, Map> mappings, ArrayList<String> tableConfig) throws JSONException {
        try {
            if (json == null || json.isEmpty()) throw new RuntimeException("Json can't be null or empty!");
            if (json.trim().startsWith("{") && json.trim().endsWith("}")) {
                JSONObject jsonObject = new JSONObject(json);
                return convertToHtmlTable(fromObject(jsonObject), mappings, tableConfig);
            } else if (json.trim().startsWith("[") && json.trim().endsWith("]")) {
                JSONArray jsonArray = new JSONArray(json);
                return convertToHtmlTable(jsonArray, mappings, tableConfig);
            }
        } catch (Exception e) {
            return null;
        }
       throw new RuntimeException("Provided value doesn't seem to be a json formatted string!");
    }

    private static JSONArray fromObject(JSONObject object) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(object);
        return jsonArray;
    }

    private static String convertToHtmlTable(JSONArray jsonArray, LinkedHashMap<String, Map> mappings, ArrayList<String> tableConfig) throws JSONException {
        if (jsonArray.isEmpty()) return "";
        Object item = jsonArray.get(0);
        if (!(item instanceof JSONObject) && !(item instanceof JSONArray))
            return item.toString();
        if (item instanceof JSONArray)
            return convertToHtmlTable((JSONArray) item, mappings, tableConfig);

        StringBuilder html = new StringBuilder();
        if (!tableConfig.get(3).toString().isEmpty()){
            html.append("<table class=\"jsonToTableFormatter\" style=\"" + tableConfig.get(3).toString() + "\">");
        } else {
            html.append("<table>");
        }

        // HEAD
        ArrayList<String> columnHeaders = generateTableHeader(jsonArray, tableConfig, mappings, html);

        // BODY
        generateTableBody(jsonArray, tableConfig, mappings, html, columnHeaders);
       
        html.append("</table>");
        return html.toString();
    }

    private static ArrayList<String> generateTableHeader(JSONArray jsonArray, ArrayList<String> tableConfig, LinkedHashMap<String, Map> mappings, StringBuilder html) throws JSONException {
        if (!tableConfig.get(2).toString().isEmpty()){
            html.append("<tr style=\"background-color: " + tableConfig.get(2).toString());
        } else {
            html.append("<tr style=\"background-color: #4CAF50");
        }

        if (!tableConfig.get(1).toString().isEmpty()){
            html.append(";color: " + tableConfig.get(1).toString() + ";\">");
        } else {
            html.append(";color: white;\">");
        }

        ArrayList<String> columnHeaders = new ArrayList<String>();
        for (int i = 0; i < jsonArray.length(); i++) {
            for (String key : jsonArray.getJSONObject(i).keySet()) {
                if (!columnHeaders.contains(key)) {
                    columnHeaders.add(key);
                }
            }
        }
   
        ArrayList<String> sortedColumnHeaders = new ArrayList<String>();
        mappings.keySet().forEach(mapKey -> {
            columnHeaders.forEach(key -> {
                if (key.equals(mapKey)) {
                    sortedColumnHeaders.add(key);
                    if(mappings.containsKey(key)){
                        if (mappings.get(key).get("hideColumn").toString().equalsIgnoreCase("true")) {
                            return;
                        }
                        if(mappings.get(key).get("columnInlineStyle") != null){
                            html.append("<th style=\"" + mappings.get(key).get("columnInlineStyle").toString() + "\">").append(mappings.get(key).get("label").toString()).append("</th>");
                        } else {
                            html.append("<th>").append(mappings.get(key).get("label").toString()).append("</th>");
                        }
                    } else {
                        if (tableConfig.get(0).toString().equalsIgnoreCase("true")) {
                            html.append("<th style=\"text-align: left;\">").append(key).append("</th>");
                        }
                    }
                }
            });
        });

        if (tableConfig.get(0).toString().equalsIgnoreCase("true")) {
            columnHeaders.removeAll(sortedColumnHeaders);
            columnHeaders.forEach(key -> {
                sortedColumnHeaders.add(key);
                html.append("<th style=\"text-align: left;\">").append(key).append("</th>");
            });
        }

        html.append("</tr>");
        return sortedColumnHeaders;
    }

    private static void generateTableBody( JSONArray jsonArray, ArrayList<String> tableConfig, LinkedHashMap<String, Map> mappings, StringBuilder html, ArrayList<String> columnHeaders) throws JSONException {
        html.append("<tbody>");
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            html.append("<tr>");

            columnHeaders.forEach(key -> {
                Object value = new Object();
                try {
                    value = obj.get(key);
                } catch (Exception e) {
                    value = "";
                }

                String toAppend;
                if (value instanceof JSONArray) {
                    toAppend = convertToHtmlTable((JSONArray) value, mappings, tableConfig);
                } else if (value instanceof JSONObject) {
                    toAppend = convertToHtmlTable(fromObject((JSONObject) value), mappings, tableConfig);
                } else {
                    toAppend = value.toString();
                }

                if (mappings.containsKey(key)) {
                    if (mappings.get(key).get("hideColumn").toString().equalsIgnoreCase("true")) {
                        return;
                    }
                    if(mappings.get(key).get("columnInlineStyle") != null){
                        html.append("<td style=\"" + mappings.get(key).get("columnInlineStyle").toString() + "\">").append(toAppend).append("</td>");
                    } else {
                        html.append("<td>").append(toAppend).append("</td>");
                    }
                } else {
                    html.append("<td>").append(toAppend).append("</td>");
                }
            });
            html.append("</tr>");
        }
        html.append("</tbody>");
    }
}
