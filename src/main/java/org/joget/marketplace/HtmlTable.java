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
            if (json == null || json.isEmpty()) {
                throw new RuntimeException("Json can't be null or empty!");
            }
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
        Boolean isTransposed = tableConfig.get(4).equalsIgnoreCase("true");
        Boolean noHeader = false;
        if (jsonArray.isEmpty()) {
            return "";
        }
        Object item = jsonArray.get(0);
        // if item is not {key:value} and not [{key:value}], e.g: [1,2,3]
        if (!(item instanceof JSONObject) && !(item instanceof JSONArray)) {
            noHeader = true;
        }
        if (item instanceof JSONArray) {
            return convertToHtmlTable((JSONArray) item, mappings, tableConfig);
        }

        StringBuilder html = new StringBuilder();
        if (!tableConfig.get(3).toString().isEmpty()) {
            html.append("<table class=\"jsonToTableFormatter\" style=\"" + tableConfig.get(3).toString() + "\">");
        } else {
            html.append("<table>");
        }

        if(noHeader){
            generateTableBodyWithoutHeader(jsonArray, html, isTransposed);
        } else {
            if (isTransposed) {
                // Transpose logic  
                LinkedHashMap<String, LinkedHashMap<String, String>> transposed = transposeJsonArray(jsonArray, tableConfig, mappings);
                // Pass transposed array to generateTableBody
                generateTransposedTable(transposed, tableConfig, mappings, html);
            } else {
                // Normal table generation
                ArrayList<String> columnHeaders = generateTableHeader(jsonArray, tableConfig, mappings, html);
                generateTableBody(jsonArray, tableConfig, mappings, html, columnHeaders);
            }    
        }
    
        html.append("</table>");
        return html.toString();
    }

    private static ArrayList<String> generateTableHeader(JSONArray jsonArray, ArrayList<String> tableConfig,
            LinkedHashMap<String, Map> mappings, StringBuilder html) throws JSONException {
        if (!tableConfig.get(2).toString().isEmpty()) {
            html.append("<tr style=\"background-color: " + tableConfig.get(2).toString());
        } else {
            html.append("<tr style=\"background-color: #4CAF50");
        }

        if (!tableConfig.get(1).toString().isEmpty()) {
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
                    if (mappings.containsKey(key)) {
                        if (mappings.get(key).get("hideColumn").toString().equalsIgnoreCase("true")) {
                            return;
                        }
                        html.append("<th>").append(mappings.get(key).get("label").toString()).append("</th>");
                    } else {
                        if (tableConfig.get(0).toString().equalsIgnoreCase("true")) {
                            html.append("<th>").append(key).append("</th>");
                        }
                    }
                }
            });
        });

        if (tableConfig.get(0).toString().equalsIgnoreCase("true")) {
            columnHeaders.removeAll(sortedColumnHeaders);
            columnHeaders.forEach(key -> {
                sortedColumnHeaders.add(key);
                html.append("<th>").append(key).append("</th>");
            });
        }

        html.append("</tr>");
        return sortedColumnHeaders;
    }

    private static void generateTableBodyWithoutHeader(JSONArray jsonArray, StringBuilder html, Boolean isTransposed) throws JSONException {
        html.append("<tbody>");
        if(isTransposed){
            for (int i = 0; i < jsonArray.length(); i++) {
                html.append("<tr><td>" + jsonArray.get(i) + "</td></tr>");
            }
        } else{
            html.append("<tr>");
            for (int i = 0; i < jsonArray.length(); i++) {
                html.append("<td>" + jsonArray.get(i) + "</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody>");
    }

    private static void generateTableBody(JSONArray jsonArray, ArrayList<String> tableConfig, LinkedHashMap<String, Map> mappings, StringBuilder html, ArrayList<String> columnHeaders) throws JSONException {
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

                String toAppend = "";
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
                    if (mappings.get(key).get("columnInlineStyle") != null) {
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

    private static void generateTransposedTable(LinkedHashMap<String, LinkedHashMap<String, String>> transposedMap, ArrayList<String> tableConfig, LinkedHashMap<String, Map> mappings, StringBuilder html) throws JSONException {

        html.append("<tbody>");

        // Set default styles
        String headerBgColor = "#4CAF50";
        if (!tableConfig.get(2).isEmpty()) {
            headerBgColor = tableConfig.get(2);
        }

        String headerFontColor = "white";
        if (!tableConfig.get(1).isEmpty()) {
            headerFontColor = tableConfig.get(1);
        }

        // Set header color
        html.append("<col style=\"background-color: " + headerBgColor + ";\">");

        // Generate rows
        for (Map.Entry<String, LinkedHashMap<String, String>> row : transposedMap.entrySet()) {
            html.append("<tr>");

            int colIndex = 0;

            for (Map.Entry<String, String> col : row.getValue().entrySet()) {

                String cellValue = col.getValue();
                String cellStyle = "";

                // Set header font color
                if (colIndex == 0) {
                    cellStyle += "font-weight: bold; color: " + headerFontColor + ";";
                }

                // Set row cell style
                if (colIndex != 0) {
                    cellStyle += getCellStyle(row.getKey(), mappings);
                }

                // Append cell
                html.append("<td style=\"" + cellStyle + "\">")
                        .append(cellValue)
                        .append("</td>");

                colIndex++;
            }

            html.append("</tr>");
        }

        html.append("</tbody>");

    }

    private static String getCellStyle(String rowKey, Map<String, Map> mappings) {
        if (mappings.containsKey(rowKey)) {
            Map rowMapping = mappings.get(rowKey);
            return (String) rowMapping.get("columnInlineStyle");
        }
        return "";
    }

    public static LinkedHashMap<String, LinkedHashMap<String, String>> transposeJsonArray(JSONArray jsonArray, ArrayList<String> tableConfig, LinkedHashMap<String, Map> mappings) throws JSONException {

        // Get column headers 
        ArrayList<String> columnHeaders = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject row = jsonArray.getJSONObject(i);
            for (String key : row.keySet()) {
                if (!columnHeaders.contains(key)) {
                    columnHeaders.add(key);
                }
            }
        }
        // Get the first object to extract keys for headers
        JSONObject first = jsonArray.getJSONObject(0);
        JSONArray headers = new JSONArray();
        for (String key : first.keySet()) {
            headers.put(key);
        }

        // Sort headers according to mappings
        ArrayList<String> sortedRowHeaders = new ArrayList<String>();
        mappings.keySet().forEach(mapKey -> {
            columnHeaders.forEach(key -> {
                if (key.equals(mapKey)) {
                    sortedRowHeaders.add(key);
                }
            });
        });

        if (tableConfig.get(0).equalsIgnoreCase("true")) {
            columnHeaders.removeAll(sortedRowHeaders);
            columnHeaders.forEach(key -> {
                sortedRowHeaders.add(key);
            });
        }

        // Initialize transposed array
        LinkedHashMap<String, LinkedHashMap<String, String>> transposedMap = new LinkedHashMap<>();

        // Loop through headers
        for (String rowHeader : sortedRowHeaders) {

            String rowLabel = rowHeader;

            if (mappings.containsKey(rowHeader)) {
                // Hide column if set to true
                if (mappings.get(rowHeader).get("hideColumn").toString().equalsIgnoreCase("true")) {
                    continue; // Skip adding this row
                }
                // Set label if input by mapping
                if (mappings.get(rowHeader).containsKey("label")) {
                    rowLabel = mappings.get(rowHeader).get("label").toString();
                }
            } else {
                // Hide unmapped rows
                if (!tableConfig.get(0).equalsIgnoreCase("true")) {
                    continue; // Skip adding this row
                }
            }

            // New row for each column header
            LinkedHashMap<String, String> newRow = new LinkedHashMap<>();

            // Loop through each row to get value for current column
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject row = jsonArray.getJSONObject(i);
                String valueIndex = "" + i;
                String cellValue = "";

                Object item = row.get(rowHeader);

                if (item instanceof JSONArray) {
                    cellValue = convertToHtmlTable((JSONArray) item, mappings, tableConfig);
                } else if (item instanceof JSONObject) {
                    cellValue = convertToHtmlTable(fromObject((JSONObject) item), mappings, tableConfig);
                } else {
                    if (row.has(rowHeader)) {
                        cellValue = item.toString();
                    }
                }

                // Row headers
                newRow.put(headers.toString(), rowLabel);
                // Cell values
                newRow.put(valueIndex, cellValue);
            }

            // Add new row to transposed array
            transposedMap.put(rowHeader, newRow);
        }

        return transposedMap;
    }
}
