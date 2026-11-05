package org.joget.marketplace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joget.apps.app.service.AppPluginUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Renders the JSON history stored by the "Workflow Form Binder with Audit Trail"
 * plugin as a readable comparison. The list shows a short summary, and a popup
 * shows every change in one Field / Row / Before / After table, where grid and
 * subform changes are grouped under their field.
 */
public class AuditTrailTable {

    private static final String MESSAGE_PATH = "messages/JSONToTableFormatter";

    //long texts are compared word by word, up to this many words per side
    private static final int MAX_DIFF_WORDS = 400;
    private static final int WORD_DIFF_MIN_LENGTH = 40;

    //fields shown in the list before "+N more"
    private static final int MAX_LIST_FIELDS = 4;

    //colours come from the Joget theme variables (with Bootstrap defaults), so
    //they follow the app's theme and its dark mode. Red only means removed, green only added.
    private static final String STYLE = "<style>"
            + ".atc{--atc-bg:var(--jgt-body-bg,#fff);--atc-fg:var(--jgt-body-color,#212529);--atc-muted:var(--jgt-secondary-color,#6c757d);"
            + "--atc-line:var(--jgt-border-color,#dee2e6);--atc-soft:var(--jgt-tertiary-bg,#f8f9fa);--atc-accent:var(--jgt-link-color,#0d6efd);"
            + "--atc-accent-soft:var(--jgt-primary-bg-subtle,#cfe2ff);--atc-red:var(--jgt-danger,#dc3545);--atc-red-soft:var(--jgt-danger-bg-subtle,#f8d7da);"
            + "--atc-red-text:var(--jgt-danger-text-emphasis,#58151c);--atc-green:var(--jgt-success,#198754);--atc-green-soft:var(--jgt-success-bg-subtle,#d1e7dd);"
            + "--atc-green-text:var(--jgt-success-text-emphasis,#0a3622);--atc-radius:var(--jgt-border-radius,.375rem);"
            + "font-size:13px;line-height:1.45;min-width:260px;max-width:100%}"
            //summary
            + ".atc .atc-sum{display:flex;flex-wrap:wrap;align-items:center;gap:6px}"
            + ".atc .atc-n{font-size:14px;font-weight:700;margin-right:4px;color:var(--atc-fg)}"
            + ".atc .atc-p{display:inline-block;padding:0 7px;border-radius:999px;font-size:11px;font-weight:600;line-height:18px;"
            + "background:var(--atc-soft);color:var(--atc-muted);border:1px solid var(--atc-line)}"
            + ".atc .atc-p.add{background:var(--atc-green-soft);color:var(--atc-green-text);border-color:transparent}"
            + ".atc .atc-p.del{background:var(--atc-red-soft);color:var(--atc-red-text);border-color:transparent}"
            + ".atc .atc-tags{display:flex;flex-wrap:wrap;gap:5px;margin-top:6px}"
            //the one field style, used in the list and in the popup
            + ".atc .atc-fld{display:inline-flex;align-items:center;gap:5px;font-weight:600;color:var(--atc-fg);white-space:nowrap}"
            + ".atc .atc-fld svg{width:13px;height:13px;flex:none;color:var(--atc-muted)}"
            + ".atc .atc-tags .atc-fld{font-weight:400;font-size:11.5px;color:var(--atc-muted);padding:0 8px;line-height:20px;border:1px solid var(--atc-line);border-radius:999px;background:var(--atc-soft)}"
            + ".atc .atc-more{font-size:12px;color:var(--atc-muted);align-self:center}"
            + ".atc .atc-foot{display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin-top:8px}"
            + ".atc .atc-who{display:inline-flex;align-items:center;gap:6px;font-size:11.5px;color:var(--atc-muted)}"
            + ".atc .atc-av{display:inline-flex;align-items:center;justify-content:center;width:20px;height:20px;border-radius:50%;"
            + "background:var(--atc-accent-soft);color:var(--atc-accent);font-size:9.5px;font-weight:700;flex:none}"
            //a standard outline button in the theme's accent colour
            + ".atc .atc-btn{display:inline-flex;align-items:center;gap:5px;margin-left:auto;padding:3px 10px;cursor:pointer;user-select:none;"
            + "font-size:12px;font-weight:500;color:var(--atc-accent);background:transparent;border:1px solid var(--atc-accent);border-radius:var(--atc-radius)}"
            + ".atc .atc-btn:hover,.atc .atc-btn:focus{background:var(--atc-accent-soft);outline:none}"
            + ".atc .atc-acts{display:inline-flex;gap:6px;margin-left:auto}.atc .atc-acts .atc-btn{margin-left:0}"
            //the secondary button is neutral, the popup button carries the accent
            + ".atc .atc-btn2{color:var(--atc-fg);border-color:var(--atc-line)}"
            + ".atc .atc-btn2:hover,.atc .atc-btn2:focus{background:var(--atc-soft)}"
            + ".atc .atc-chev{display:inline-block;font-size:10px;transition:transform .15s}"
            + ".atc.atc-open .atc-chev{transform:rotate(180deg)}"
            + ".atc .atc-off,.atc.atc-open .atc-on{display:none}.atc.atc-open .atc-off{display:inline}"
            + ".atc .atc-inline{display:none;margin-top:10px;border:1px solid var(--atc-line);border-radius:var(--atc-radius);overflow:auto}"
            + ".atc.atc-open{min-width:min(420px,90vw)}.atc.atc-open .atc-inline{display:block}"
            //popup
            + ".atc dialog.atc-dlg{padding:0;border:1px solid var(--atc-line);border-radius:var(--jgt-border-radius-lg,.5rem);width:min(900px,94vw);"
            + "max-height:86vh;overflow:hidden;background:var(--atc-bg);color:var(--atc-fg);box-shadow:0 12px 40px rgba(0,0,0,.18);text-align:left;font-size:13px}"
            + ".atc dialog.atc-dlg[open]{display:flex;flex-direction:column}"
            + ".atc dialog.atc-dlg::backdrop{background:rgba(0,0,0,.4)}"
            + ".atc .atc-dh{display:flex;align-items:flex-start;gap:12px;padding:18px 20px 14px}"
            + ".atc .atc-dh h3{margin:0;font-size:19px;font-weight:700;letter-spacing:-.01em;color:var(--atc-fg)}"
            + ".atc .atc-dh .atc-sub{margin-top:3px;font-size:12px;color:var(--atc-muted)}"
            + ".atc .atc-x{margin-left:auto;width:30px;height:30px;flex:none;display:inline-flex;align-items:center;justify-content:center;cursor:pointer;"
            + "user-select:none;font-size:18px;line-height:1;color:var(--atc-muted);border:1px solid var(--atc-line);border-radius:var(--atc-radius)}"
            + ".atc .atc-x:hover,.atc .atc-x:focus{background:var(--atc-soft);color:var(--atc-fg);outline:none}"
            + ".atc .atc-ds{padding:0 20px 14px;border-bottom:1px solid var(--atc-line)}"
            + ".atc .atc-db{overflow:auto}"
            + ".atc .atc-legend{padding:10px 20px;border-top:1px solid var(--atc-line);font-size:11px;color:var(--atc-muted)}"
            //the changes table
            + ".atc table.atc-t{border-collapse:collapse;width:100%;margin:0;background:transparent}"
            + ".atc table.atc-t th,.atc table.atc-t td{border:0!important;border-bottom:1px solid var(--atc-line)!important;padding:9px 12px!important;"
            + "vertical-align:top;text-align:left;white-space:normal;max-width:none!important;background:transparent!important;color:var(--atc-fg)!important}"
            + ".atc table.atc-t th{font-size:10.5px;font-weight:600;text-transform:uppercase;letter-spacing:.06em;color:var(--atc-muted)!important;padding-top:7px!important;padding-bottom:7px!important;"
            + "background:var(--atc-soft)!important;position:sticky;top:0;z-index:1}"
            + ".atc table.atc-t th:nth-child(1){width:26%}.atc table.atc-t th:nth-child(2){width:7%}"
            + ".atc table.atc-t th:nth-child(3),.atc table.atc-t th:nth-child(4){width:33.5%}"
            + ".atc table.atc-t td.atc-rn{font-size:12px;color:var(--atc-muted)!important}"
            + ".atc table.atc-t .atc-fld{font-size:13.5px;font-weight:700}"
            + ".atc .atc-fh{display:inline-flex;align-items:center}"
            //sub lines start where the parent's name starts: cell padding + arrow slot + icon and gap
            + ".atc table.atc-t td.atc-in{padding-left:44px!important;font-size:12.5px;font-weight:400;color:var(--atc-muted)!important}"
            + ".atc tr.atc-gh td{cursor:pointer;user-select:none}"
            + ".atc tr.atc-gh:hover td{background:var(--atc-soft)!important}"
            + ".atc .atc-caret{display:inline-block;width:14px;flex:none;font-size:10px;color:var(--atc-muted);transition:transform .15s}"
            + ".atc tbody.atc-closed .atc-caret{transform:rotate(-90deg)}"
            + ".atc tbody.atc-closed tr.atc-r{display:none}"
            + ".atc table.atc-t td.atc-cnt{text-align:right;font-size:11.5px;font-weight:400;color:var(--atc-muted)!important;vertical-align:middle}"
            //values: red struck through before, green after
            + ".atc .atc-del,.atc del{color:var(--atc-red);font-size:12.5px;opacity:.85;text-decoration:line-through;background:transparent}"
            + ".atc .atc-ins,.atc ins{color:var(--atc-green);font-size:13.5px;font-weight:700;text-decoration:none;background:transparent}"
            + ".atc .atc-w del{background:var(--atc-red-soft)}.atc .atc-w ins{background:var(--atc-green-soft)}"
            //inside long texts only the changed words are marked, at the text size
            + ".atc .atc-w del,.atc .atc-w ins{font-size:inherit;opacity:1}"
            + ".atc .atc-e{color:var(--atc-muted)}"
            + ".atc .atc-k{color:var(--atc-muted)}"
            + ".atc .atc-p.tag{margin-left:6px;vertical-align:1px}"
            + "@media (max-width:600px){.atc table.atc-t thead{display:none}.atc table.atc-t td{display:block;width:auto!important;border-bottom:0!important;padding:4px 14px!important}"
            + ".atc table.atc-t tr{display:block;border-bottom:1px solid var(--atc-line)}"
            + ".atc table.atc-t td[data-l]:before{content:attr(data-l)': ';color:var(--atc-muted)}.atc .atc-dh,.atc .atc-ds{padding:12px 14px}}"
            + "</style>";

    private static final String ICON_FIELD = "<svg viewBox=\"0 0 16 16\"><path fill=\"currentColor\" d=\"M12.1 1.3l2.6 2.6-8.6 8.6H3.5V9.9zM2 14h12v1.5H2z\"/></svg>";
    private static final String ICON_FILE = "<svg viewBox=\"0 0 16 16\"><path fill=\"currentColor\" d=\"M10.5 2a3 3 0 013 3v6.2a5 5 0 01-10 0V5h1.5v6.2a3.5 3.5 0 007 0V5a1.5 1.5 0 00-3 0v6a.5.5 0 001 0V5.5h1.5V11a2 2 0 01-4 0V5a3 3 0 013-3z\"/></svg>";
    private static final String ICON_GRID = "<svg viewBox=\"0 0 16 16\"><path fill=\"currentColor\" d=\"M1 2h14v12H1zm1.5 1.5V6h4V3.5zm5.5 0V6h5.5V3.5zM2.5 7.5V10h4V7.5zm5.5 0V10h5.5V7.5zM2.5 11.5v1h4v-1zm5.5 0v1h5.5v-1z\"/></svg>";
    private static final String ICON_SUBFORM = "<svg viewBox=\"0 0 16 16\"><path fill=\"currentColor\" d=\"M1 1h10v3H9.5V2.5h-7v7H4V11H1zm4 4h10v10H5zm1.5 1.5v7h7v-7z\"/></svg>";

    private static final String NONE = "&mdash;";

    private final String className;
    private final boolean showFieldId;
    //whether the changes start expanded in the row
    private final boolean inline;

    /** Counts of the table rows by kind of change. */
    protected static class Stats {
        int updated;
        int added;
        int removed;
    }

    /** One line of the changes table. */
    protected static class Line {
        String field;
        String row;
        String before;
        String after;

        Line(String field, String row, String before, String after) {
            this.field = field;
            this.row = row;
            this.before = before;
            this.after = after;
        }
    }

    public AuditTrailTable(String className, boolean showFieldId, boolean inline) {
        this.className = className;
        this.showFieldId = showFieldId;
        this.inline = inline;
    }

    /**
     * Returns the comparison as HTML, or null when the value is not an audit
     * trail changes array.
     *
     * @param author name of the user who made the change, may be null
     * @param date when the change was made, as displayed in the list, may be null
     * @param includeStyle whether to include the CSS (only needed once per page)
     */
    public String render(String json, String author, Object date, boolean includeStyle) {
        JSONArray changes = parse(json);
        if (changes == null) {
            return null;
        }

        Stats stats = new Stats();
        StringBuilder groups = new StringBuilder();
        for (int i = 0; i < changes.length(); i++) {
            JSONObject change = changes.optJSONObject(i);
            if (change != null) {
                renderGroup(change, stats, groups);
            }
        }

        String who = authorHtml(author, date);
        String summary = summaryHtml(changes.length(), stats);

        StringBuilder html = new StringBuilder();
        if (includeStyle) {
            html.append(STYLE);
        }
        html.append("<div class=\"atc").append(inline ? " atc-open" : "").append("\"><div class=\"atc-sum\">").append(summary).append("</div>");
        if (changes.length() == 0) {
            html.append("<div class=\"atc-foot\">").append(who).append("</div></div>");
            return html.toString();
        }

        //the changed fields, in the same style as in the table
        html.append("<div class=\"atc-tags\">");
        for (int i = 0; i < changes.length() && i < MAX_LIST_FIELDS; i++) {
            html.append(fieldHtml(changes.optJSONObject(i)));
        }
        if (changes.length() > MAX_LIST_FIELDS) {
            html.append("<span class=\"atc-more\">").append(escape(format(getMessage("more"), changes.length() - MAX_LIST_FIELDS))).append("</span>");
        }
        html.append("</div>");

        String table = "<table class=\"atc-t\"><thead><tr><th>" + escape(getMessage("field")) + "</th><th>" + escape(getMessage("row"))
                + "</th><th>" + escape(getMessage("before")) + "</th><th>" + escape(getMessage("after")) + "</th></tr></thead>"
                + groups + "</table>";

        //spans instead of buttons: the datalist binds its own click handler to buttons inside the list form.
        //The popup is a native dialog and expanding only toggles a class, so no script needs to be loaded.
        String keys = " onkeydown=\"if(event.key==='Enter'||event.key===' '){this.click();return false;}\"";
        html.append("<div class=\"atc-foot\">").append(who).append("<span class=\"atc-acts\">")
                .append("<span role=\"button\" tabindex=\"0\" class=\"atc-btn atc-btn2\"").append(keys)
                .append(" onclick=\"event.preventDefault();event.stopPropagation();this.closest('.atc').classList.toggle('atc-open');\">")
                .append("<span class=\"atc-chev\">&#9662;</span><span class=\"atc-on\">").append(escape(getMessage("expand")))
                .append("</span><span class=\"atc-off\">").append(escape(getMessage("collapse"))).append("</span></span>")
                .append("<span role=\"button\" tabindex=\"0\" class=\"atc-btn\"").append(keys)
                .append(" onclick=\"event.preventDefault();event.stopPropagation();")
                .append("var d=this.closest('.atc').querySelector('dialog');if(d.showModal){d.showModal();}else{d.setAttribute('open','');}\">")
                .append(escape(getMessage("viewChanges"))).append("</span></span></div>")
                .append("<div class=\"atc-inline\">").append(table).append("</div>");

        html.append("<dialog class=\"atc-dlg\" onclick=\"event.stopPropagation();if(event.target===this){this.close();}\">")
                .append("<div class=\"atc-dh\"><div><h3>").append(escape(getMessage("changeDetails"))).append("</h3>");
        String subtitle = subtitle(author, date);
        if (!subtitle.isEmpty()) {
            html.append("<div class=\"atc-sub\">").append(subtitle).append("</div>");
        }
        html.append("</div><span role=\"button\" tabindex=\"0\" class=\"atc-x\" title=\"").append(escape(getMessage("close")))
                .append("\" onkeydown=\"if(event.key==='Enter'||event.key===' '){this.click();return false;}\" ")
                .append("onclick=\"event.preventDefault();event.stopPropagation();this.closest('dialog').close();\">&times;</span></div>")
                .append("<div class=\"atc-ds atc-sum\">").append(summary).append("</div>")
                .append("<div class=\"atc-db\">").append(table).append("</div>")
                .append("<div class=\"atc-legend\">").append(escape(getMessage("legend"))).append("</div>")
                .append("</dialog></div>");
        return html.toString();
    }

    /**
     * Returns the comparison as plain text lines for exports, or null when the
     * value is not an audit trail changes array.
     */
    public String renderText(String json) {
        JSONArray changes = parse(json);
        if (changes == null) {
            return null;
        }
        if (changes.length() == 0) {
            return getMessage("noChanges");
        }
        StringBuilder text = new StringBuilder();
        renderText(changes, "", null, text);
        return text.toString().trim();
    }

    protected JSONArray parse(String json) {
        if (json == null || !json.trim().startsWith("[")) {
            return null;
        }
        try {
            JSONArray changes = new JSONArray(json.trim());
            //only treat arrays of change objects as audit trail data
            for (int i = 0; i < changes.length(); i++) {
                JSONObject change = changes.optJSONObject(i);
                if (change == null || !change.has("fieldID")) {
                    return null;
                }
            }
            return changes;
        } catch (Exception ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------- summary

    protected String summaryHtml(int count, Stats stats) {
        if (count == 0) {
            return "<span class=\"atc-e\">" + escape(getMessage("noChanges")) + "</span>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<span class=\"atc-n\">").append(escape(format(getMessage(count == 1 ? "fieldChanged" : "fieldsChanged"), count))).append("</span>");
        if (stats.updated > 0) {
            html.append("<span class=\"atc-p\">").append(escape(format(getMessage("updatedCount"), stats.updated))).append("</span>");
        }
        if (stats.removed > 0) {
            html.append("<span class=\"atc-p del\">").append(escape(format(getMessage("removedCount"), stats.removed))).append("</span>");
        }
        if (stats.added > 0) {
            html.append("<span class=\"atc-p add\">").append(escape(format(getMessage("addedCount"), stats.added))).append("</span>");
        }
        return html.toString();
    }

    protected String authorHtml(String author, Object date) {
        String when = (date != null) ? date.toString() : "";
        if ((author == null || author.isEmpty()) && when.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<span class=\"atc-who\">");
        if (author != null && !author.isEmpty()) {
            html.append("<span class=\"atc-av\">").append(escape(initials(author))).append("</span>").append(escape(author));
        }
        if (!when.isEmpty()) {
            html.append(author != null && !author.isEmpty() ? " &middot; " : "").append(escape(when));
        }
        return html.append("</span>").toString();
    }

    protected String subtitle(String author, Object date) {
        List<String> parts = new ArrayList<String>();
        if (author != null && !author.isEmpty()) {
            parts.add(escape(author));
        }
        if (date != null && !date.toString().isEmpty()) {
            parts.add(escape(date.toString()));
        }
        return String.join(" &middot; ", parts);
    }

    // ---------------------------------------------------------------- table

    /**
     * Renders one changed field as a table body. A field with several lines
     * (grids, subforms) gets a header row that collapses its lines.
     */
    protected void renderGroup(JSONObject change, Stats stats, StringBuilder html) {
        String type = change.optString("fieldType", "field");
        List<Line> lines = new ArrayList<Line>();
        collectLines(change, "", null, stats, lines);
        if (lines.isEmpty()) {
            return;
        }

        if (lines.size() == 1 && !"grid".equals(type) && !"subform".equals(type)) {
            Line line = lines.get(0);
            html.append("<tbody>");
            appendLine(fieldCell(change, false), line, false, html);
            html.append("</tbody>");
            return;
        }

        html.append("<tbody class=\"atc-grp\"><tr class=\"atc-gh\" onclick=\"this.parentNode.classList.toggle('atc-closed');\">")
                .append("<td>").append(fieldCell(change, true)).append("</td>")
                //the count sits at the same place on every group row
                .append("<td colspan=\"3\" class=\"atc-cnt\">").append(escape(format(getMessage(lines.size() == 1 ? "oneChange" : "manyChanges"), lines.size())))
                .append("</td></tr>");
        for (Line line : lines) {
            appendLine(escape(line.field), line, true, html);
        }
        html.append("</tbody>");
    }

    /**
     * The field name in the table, after a fixed arrow slot (empty when the field
     * does not collapse) so icons and names line up in one column.
     */
    protected String fieldCell(JSONObject change, boolean collapsible) {
        return "<span class=\"atc-fh\"><span class=\"atc-caret\">" + (collapsible ? "&#9662;" : "") + "</span>" + fieldHtml(change) + "</span>";
    }

    protected void appendLine(String field, Line line, boolean nested, StringBuilder html) {
        html.append("<tr class=\"atc-r\"><td").append(nested ? " class=\"atc-in\"" : "").append(">").append(field)
                .append("</td><td class=\"atc-rn\" data-l=\"").append(escape(getMessage("row"))).append("\">").append(line.row)
                .append("</td><td data-l=\"").append(escape(getMessage("before"))).append("\">").append(line.before)
                .append("</td><td data-l=\"").append(escape(getMessage("after"))).append("\">").append(line.after)
                .append("</td></tr>");
    }

    /**
     * Flattens a change into table lines: grids give one line per changed cell,
     * added or removed row, subforms one line per changed field.
     */
    protected void collectLines(JSONObject change, String prefix, Map<String, String> columns, Stats stats, List<Line> lines) {
        String type = change.optString("fieldType", "field");
        String label = prefix + getLabel(change, columns);

        if ("subform".equals(type)) {
            JSONArray inner = change.optJSONArray("changes");
            for (int i = 0; inner != null && i < inner.length(); i++) {
                JSONObject child = inner.optJSONObject(i);
                if (child == null) {
                    continue;
                }
                String childType = child.optString("fieldType", "field");
                //nested grids and subforms keep their name as a prefix of their lines
                boolean container = "grid".equals(childType) || "subform".equals(childType);
                collectLines(child, container ? prefix + getLabel(child, null) + " › " : prefix, null, stats, lines);
            }
        } else if ("grid".equals(type)) {
            collectGridLines(change, prefix, stats, lines);
        } else if ("file".equals(type)) {
            String[] values = fileValues(change);
            int added = length(change.optJSONArray("filesAdded"));
            int removed = length(change.optJSONArray("filesRemoved"));
            int replaced = length(change.optJSONArray("filesReplaced"));
            if (replaced == 0 && added == 0 && removed > 0) {
                stats.removed++;
            } else if (replaced == 0 && removed == 0 && added > 0) {
                stats.added++;
            } else {
                stats.updated++;
            }
            lines.add(new Line(label, NONE, values[0], values[1]));
        } else {
            String before = change.optString("beforeContentValue");
            String after = change.optString("afterContentValue");
            if (before.isEmpty() && !after.isEmpty()) {
                stats.added++;
            } else if (!before.isEmpty() && after.isEmpty()) {
                stats.removed++;
            } else {
                stats.updated++;
            }
            String[] values = textValues(before, after);
            lines.add(new Line(label, NONE, values[0], values[1]));
        }
    }

    protected void collectGridLines(JSONObject change, String prefix, Stats stats, List<Line> lines) {
        Map<String, String> columns = gridColumns(change);

        //added and modified rows in grid order, then the deleted rows
        List<JSONObject> rows = new ArrayList<JSONObject>();
        final Map<JSONObject, String> states = new LinkedHashMap<JSONObject, String>();
        addRows(change.optJSONArray("rowsAdded"), "add", rows, states);
        addRows(change.optJSONArray("rowsModified"), "mod", rows, states);
        Collections.sort(rows, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                return a.optInt("rowNumber", 0) - b.optInt("rowNumber", 0);
            }
        });
        addRows(change.optJSONArray("rowsDeleted"), "del", rows, states);

        for (JSONObject row : rows) {
            String state = states.get(row);
            int number = row.optInt("rowNumber", 0);
            String rowNumber = number > 0 ? String.valueOf(number) : NONE;
            if ("add".equals(state)) {
                stats.added++;
                lines.add(new Line(prefix + getMessage("newRow"), rowNumber, empty(),
                        "<span class=\"atc-ins\">" + rowValuesHtml(row, columns) + "</span>" + pill("add", getMessage("added"))));
            } else if ("del".equals(state)) {
                stats.removed++;
                lines.add(new Line(prefix + getMessage("removedRow"), rowNumber,
                        "<span class=\"atc-del\">" + rowValuesHtml(row, columns) + "</span>", pill("del", getMessage("removed"))));
            } else {
                JSONArray cells = row.optJSONArray("changes");
                for (int i = 0; cells != null && i < cells.length(); i++) {
                    JSONObject cell = cells.optJSONObject(i);
                    if (cell == null) {
                        continue;
                    }
                    int before = lines.size();
                    collectLines(cell, prefix, columns, stats, lines);
                    for (int j = before; j < lines.size(); j++) {
                        lines.get(j).row = rowNumber;
                    }
                }
            }
        }
    }

    protected void addRows(JSONArray source, String state, List<JSONObject> rows, Map<JSONObject, String> states) {
        for (int i = 0; source != null && i < source.length(); i++) {
            JSONObject row = source.optJSONObject(i);
            if (row != null) {
                rows.add(row);
                states.put(row, state);
            }
        }
    }

    protected Map<String, String> gridColumns(JSONObject change) {
        Map<String, String> columns = new LinkedHashMap<String, String>();
        JSONArray labels = change.optJSONArray("columns");
        for (int i = 0; labels != null && i < labels.length(); i++) {
            JSONObject column = labels.optJSONObject(i);
            if (column != null && !column.optString("id").isEmpty()) {
                columns.put(column.optString("id"), column.optString("label", column.optString("id")));
            }
        }
        return columns;
    }

    /** "Name: height · Value: 2", in the grid's column order. */
    protected String rowValuesHtml(JSONObject row, Map<String, String> columns) {
        JSONObject values = row.optJSONObject("values");
        if (values == null || values.length() == 0) {
            return empty();
        }
        Set<String> keys = new LinkedHashSet<String>();
        for (String key : columns.keySet()) {
            if (values.has(key)) {
                keys.add(key);
            }
        }
        Iterator<?> others = values.keys();
        while (others.hasNext()) {
            keys.add(others.next().toString());
        }
        List<String> parts = new ArrayList<String>();
        for (String key : keys) {
            String label = columns.containsKey(key) ? columns.get(key) : key;
            parts.add("<span class=\"atc-k\">" + escape(label) + ":</span> " + valueHtml(values.optString(key)));
        }
        return String.join(" &middot; ", parts);
    }

    // ---------------------------------------------------------------- values

    /**
     * Returns the before and after HTML of a text value. Long texts are compared
     * word by word so only the changed words are highlighted.
     */
    protected String[] textValues(String before, String after) {
        if (before.isEmpty() || after.isEmpty()
                || (before.length() < WORD_DIFF_MIN_LENGTH && after.length() < WORD_DIFF_MIN_LENGTH)) {
            return new String[]{
                before.isEmpty() ? empty() : "<span class=\"atc-del\">" + valueHtml(before) + "</span>",
                after.isEmpty() ? empty() : "<span class=\"atc-ins\">" + valueHtml(after) + "</span>"
            };
        }

        String[] a = before.split("(?<=\\s)");
        String[] b = after.split("(?<=\\s)");
        if (a.length > MAX_DIFF_WORDS || b.length > MAX_DIFF_WORDS) {
            return new String[]{"<span class=\"atc-del\">" + valueHtml(before) + "</span>", "<span class=\"atc-ins\">" + valueHtml(after) + "</span>"};
        }

        //longest common subsequence of words
        int[][] lcs = new int[a.length + 1][b.length + 1];
        for (int i = a.length - 1; i >= 0; i--) {
            for (int j = b.length - 1; j >= 0; j--) {
                lcs[i][j] = a[i].equals(b[j]) ? lcs[i + 1][j + 1] + 1 : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
            }
        }
        StringBuilder left = new StringBuilder("<span class=\"atc-w\">");
        StringBuilder right = new StringBuilder("<span class=\"atc-w\">");
        int i = 0;
        int j = 0;
        while (i < a.length || j < b.length) {
            if (i < a.length && j < b.length && a[i].equals(b[j])) {
                left.append(valueHtml(a[i]));
                right.append(valueHtml(b[j]));
                i++;
                j++;
            } else if (j < b.length && (i == a.length || lcs[i][j + 1] >= lcs[i + 1][j])) {
                right.append("<ins>").append(valueHtml(b[j])).append("</ins>");
                j++;
            } else {
                left.append("<del>").append(valueHtml(a[i])).append("</del>");
                i++;
            }
        }
        return new String[]{left.append("</span>").toString(), right.append("</span>").toString()};
    }

    protected String[] fileValues(JSONObject change) {
        Set<String> added = toSet(change.optJSONArray("filesAdded"));
        Set<String> removed = toSet(change.optJSONArray("filesRemoved"));
        Set<String> replaced = toSet(change.optJSONArray("filesReplaced"));
        return new String[]{
            fileList(change.optString("beforeContentValue"), removed, null, true),
            fileList(change.optString("afterContentValue"), added, replaced, false)
        };
    }

    protected String fileList(String files, Set<String> marked, Set<String> replaced, boolean before) {
        StringBuilder html = new StringBuilder();
        for (String file : files.split(";")) {
            if (file.trim().isEmpty()) {
                continue;
            }
            if (html.length() > 0) {
                html.append("<br/>");
            }
            if (replaced != null && replaced.contains(file)) {
                html.append(escape(file)).append(pill("", getMessage("replaced")));
            } else if (marked.contains(file)) {
                html.append("<span class=\"atc-").append(before ? "del" : "ins").append("\">").append(escape(file)).append("</span>")
                        .append(pill(before ? "del" : "add", getMessage(before ? "removed" : "added")));
            } else {
                html.append(escape(file));
            }
        }
        return html.length() > 0 ? html.toString() : empty();
    }

    protected String valueHtml(String value) {
        return escape(value).replace(";", "; ").replace("\n", "<br/>");
    }

    protected String empty() {
        return "<span class=\"atc-e\">" + NONE + "</span>";
    }

    protected String pill(String type, String label) {
        return "<span class=\"atc-p tag" + (type.isEmpty() ? "" : " " + type) + "\">" + escape(label) + "</span>";
    }

    /** The field name with its type icon, styled the same in the list and the popup. */
    protected String fieldHtml(JSONObject change) {
        String type = change.optString("fieldType", "field");
        String fieldId = change.optString("fieldID");
        String label = getLabel(change, null);
        StringBuilder html = new StringBuilder("<span class=\"atc-fld\">").append(icon(type)).append(escape(label));
        if (showFieldId && !fieldId.isEmpty() && !fieldId.equals(label)) {
            html.append(" <span class=\"atc-k\">(").append(escape(fieldId)).append(")</span>");
        }
        return html.append("</span>").toString();
    }

    protected String icon(String type) {
        if ("file".equals(type)) {
            return ICON_FILE;
        } else if ("grid".equals(type)) {
            return ICON_GRID;
        } else if ("subform".equals(type)) {
            return ICON_SUBFORM;
        }
        return ICON_FIELD;
    }

    protected String getLabel(JSONObject change, Map<String, String> columns) {
        String fieldId = change.optString("fieldID");
        String label = change.optString("fieldLabel");
        if (columns != null && columns.containsKey(fieldId)) {
            label = columns.get(fieldId);
        }
        return label.isEmpty() ? fieldId : label;
    }

    // ---------------------------------------------------------------- text export

    protected void renderText(JSONArray changes, String prefix, Map<String, String> columns, StringBuilder text) {
        for (int i = 0; i < changes.length(); i++) {
            JSONObject change = changes.optJSONObject(i);
            if (change == null) {
                continue;
            }
            String type = change.optString("fieldType", "field");
            String label = prefix + getLabel(change, columns);
            if ("subform".equals(type)) {
                JSONArray inner = change.optJSONArray("changes");
                if (inner != null) {
                    renderText(inner, label + " > ", null, text);
                }
            } else if ("grid".equals(type)) {
                Map<String, String> gridColumns = gridColumns(change);
                textRows(change.optJSONArray("rowsAdded"), label, getMessage("row.add"), gridColumns, text);
                JSONArray modified = change.optJSONArray("rowsModified");
                for (int r = 0; modified != null && r < modified.length(); r++) {
                    JSONObject row = modified.optJSONObject(r);
                    JSONArray rowChanges = (row != null) ? row.optJSONArray("changes") : null;
                    if (rowChanges != null) {
                        renderText(rowChanges, label + " > " + getMessage("row") + " " + row.optInt("rowNumber", 0) + " > ", gridColumns, text);
                    }
                }
                textRows(change.optJSONArray("rowsDeleted"), label, getMessage("row.del"), gridColumns, text);
            } else if ("file".equals(type)) {
                text.append(label).append(": ");
                appendFiles(text, getMessage("added"), change.optJSONArray("filesAdded"));
                appendFiles(text, getMessage("removed"), change.optJSONArray("filesRemoved"));
                appendFiles(text, getMessage("replaced"), change.optJSONArray("filesReplaced"));
                text.append("\n");
            } else {
                text.append(label).append(": ").append(textOrDash(change.optString("beforeContentValue")))
                        .append(" -> ").append(textOrDash(change.optString("afterContentValue"))).append("\n");
            }
        }
    }

    protected void textRows(JSONArray rows, String label, String state, Map<String, String> columns, StringBuilder text) {
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject row = rows.optJSONObject(i);
            JSONObject values = (row != null) ? row.optJSONObject("values") : null;
            text.append(label).append(" > ").append(getMessage("row"));
            if (row != null && row.optInt("rowNumber", 0) > 0) {
                text.append(" ").append(row.optInt("rowNumber"));
            }
            text.append(" (").append(state).append(")");
            if (values != null) {
                //grid column order first, then any other value
                Set<String> keys = new LinkedHashSet<String>();
                for (String key : columns.keySet()) {
                    if (values.has(key)) {
                        keys.add(key);
                    }
                }
                Iterator<?> others = values.keys();
                while (others.hasNext()) {
                    keys.add(others.next().toString());
                }
                List<String> parts = new ArrayList<String>();
                for (String key : keys) {
                    parts.add((columns.containsKey(key) ? columns.get(key) : key) + ": " + values.optString(key));
                }
                text.append(": ").append(String.join(", ", parts));
            }
            text.append("\n");
        }
    }

    protected void appendFiles(StringBuilder text, String label, JSONArray files) {
        if (files != null && files.length() > 0) {
            List<String> names = new ArrayList<String>();
            for (int i = 0; i < files.length(); i++) {
                names.add(files.optString(i));
            }
            text.append(label).append(" ").append(String.join(", ", names)).append("  ");
        }
    }

    protected String textOrDash(String value) {
        return value.isEmpty() ? "-" : value;
    }

    // ---------------------------------------------------------------- helpers

    protected String initials(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.toString();
    }

    protected Set<String> toSet(JSONArray array) {
        Set<String> set = new LinkedHashSet<String>();
        for (int i = 0; array != null && i < array.length(); i++) {
            set.add(array.optString(i));
        }
        return set;
    }

    protected int length(JSONArray array) {
        return (array != null) ? array.length() : 0;
    }

    protected String format(String message, Object value) {
        return message.replace("{0}", String.valueOf(value));
    }

    protected String getMessage(String key) {
        return AppPluginUtil.getMessage("datalist.JSONToTableFormatter.audit." + key, className, MESSAGE_PATH);
    }

    protected static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
