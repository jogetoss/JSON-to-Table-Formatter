package org.joget.marketplace;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class HtmlTable {
    private HtmlTable() {
    }

    public static String fromJson(String json, LinkedHashMap<String, Map> mappings) throws JSONException {
        if (json == null || json.isEmpty()) throw new RuntimeException("Json can't be null or empty!");
        if (json.trim().startsWith("{")) {
            JSONObject jsonObject = new JSONObject(json);
            return convertToHtmlTable(fromObject(jsonObject), mappings);
        } else if (json.trim().startsWith("[")) {
            JSONArray jsonArray = new JSONArray(json);
            return convertToHtmlTable(jsonArray, mappings);
        }
        throw new RuntimeException("Provided value doesn't seem to be a json formatted string!");
    }

    private static JSONArray fromObject(JSONObject object) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(object);
        return jsonArray;
    }

    private static String convertToHtmlTable(JSONArray jsonArray, LinkedHashMap<String, Map> mappings) throws JSONException {
        if (jsonArray.isEmpty()) return "";
        Object item = jsonArray.get(0);
        if (!(item instanceof JSONObject) && !(item instanceof JSONArray))
            return item.toString();
        if (item instanceof JSONArray)
            return convertToHtmlTable((JSONArray) item, mappings);

        Set<String> keys = jsonArray.getJSONObject(0).keySet();
        StringBuilder html = new StringBuilder();
        html.append("<table style=\"border: 1px solid #ddd; padding: 8px;\">");

        // HEAD
        html.append("<tr style=\"background-color: #4CAF50; color: white;padding-top: 5px; padding-bottom: 5px;\">");
        mappings.keySet().forEach(mapKey -> {
            keys.forEach(key -> {
                if (key.equals(mapKey)) {
                    if (mappings.get(mapKey).get("hideColumn").toString().equalsIgnoreCase("false")) {
                        html.append("<th style=\"text-align: left;\">").append(mappings.get(mapKey).get("label").toString()).append("</th>");
                    } else {
                       return;
                    }
                }
            });
        });
        html.append("</tr>");

        // BODY
        html.append("<tbody>");

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            html.append("<tr>");
            mappings.keySet().forEach(mapKey -> {
                keys.forEach(key -> {
                    if (key.equals(mapKey)) {
                        if (mappings.get(mapKey).get("hideColumn").toString().equalsIgnoreCase("true")) {
                            return;
                        }
                        Object value = obj.get(mapKey);
                        String toAppend;
                        if (value instanceof JSONArray) {
                            toAppend = convertToHtmlTable((JSONArray) value, mappings);
                        } else if (value instanceof JSONObject) {
                            toAppend = convertToHtmlTable(fromObject((JSONObject) value), mappings);
                        } else {
                            toAppend = value.toString();
                        }
                        html.append("<td>").append(toAppend).append("</td>");
                    }
                });
            });
            html.append("</tr>");
        }
        html.append("</tbody>");

        html.append("</table>");
        return html.toString();
    }
}
