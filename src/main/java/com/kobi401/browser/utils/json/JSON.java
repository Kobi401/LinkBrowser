package com.kobi401.browser.utils.json;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class JSON {

    private Map<String, Object> map;

    //empty map
    public JSON() {
        this.map = new HashMap<>();
    }

    //a key-value pair to the JSON object
    public JSON put(String key, Object value) {
        map.put(key, value);
        return this; // Allow method chaining
    }

    //convert the JSON object to a string
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        boolean first = true;

        for (Map.Entry<String, Object> entry : entrySet) {
            if (!first) {
                sb.append(", ");
            }

            String key = entry.getKey();
            Object value = entry.getValue();

            //add the key
            sb.append("\"").append(key).append("\": ");

            //handle value types
            if (value instanceof String) {
                sb.append("\"").append(escapeString((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof JSON) {
                sb.append(((JSON) value).toString());
            } else {
                sb.append("null");
            }

            first = false;
        }

        sb.append("}");
        return sb.toString();
    }

    //escape special characters in a string for valid JSON formatting
    private String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    //get the underlying map (used for internal processing, if needed)
    public Map<String, Object> getMap() {
        return map;
    }

    //utility method to create a JSON from a string (parsing manually)
    public static JSON fromString(String jsonString) {
        JSON json = new JSON();
        int i = 0;
        jsonString = jsonString.trim();

        //check for the opening curly brace
        if (jsonString.charAt(i) == '{') {
            i++; //skip the opening brace
            while (i < jsonString.length()) {
                //skip white spaces
                while (i < jsonString.length() && Character.isWhitespace(jsonString.charAt(i))) {
                    i++;
                }

                if (i >= jsonString.length() || jsonString.charAt(i) == '}') {
                    break;
                }

                //read the key (should be inside quotes)
                StringBuilder keyBuilder = new StringBuilder();
                if (jsonString.charAt(i) == '\"') {
                    i++; //skip the opening quote
                    while (i < jsonString.length() && jsonString.charAt(i) != '\"') {
                        keyBuilder.append(jsonString.charAt(i));
                        i++;
                    }
                    i++; //skip the closing quote
                }
                String key = keyBuilder.toString();

                //skip spaces
                while (i < jsonString.length() && Character.isWhitespace(jsonString.charAt(i))) {
                    i++;
                }

                //skip the colon separator
                if (i < jsonString.length() && jsonString.charAt(i) == ':') {
                    i++;
                }

                //skip spaces again
                while (i < jsonString.length() && Character.isWhitespace(jsonString.charAt(i))) {
                    i++;
                }

                //read the value (could be a string, number, object, or null)
                Object value = null;

                if (i < jsonString.length()) {
                    char currentChar = jsonString.charAt(i);

                    //if it's a quote, it's a string
                    if (currentChar == '\"') {
                        i++;
                        StringBuilder valueBuilder = new StringBuilder();
                        while (i < jsonString.length() && jsonString.charAt(i) != '\"') {
                            valueBuilder.append(jsonString.charAt(i));
                            i++;
                        }
                        i++; //skip the closing quote
                        value = valueBuilder.toString();
                    }
                    //if it's a digit or '-' it's a number
                    else if ((currentChar >= '0' && currentChar <= '9') || currentChar == '-') {
                        StringBuilder valueBuilder = new StringBuilder();
                        while (i < jsonString.length() && (Character.isDigit(jsonString.charAt(i)) || jsonString.charAt(i) == '.')) {
                            valueBuilder.append(jsonString.charAt(i));
                            i++;
                        }
                        value = Double.parseDouble(valueBuilder.toString());
                    }
                    //if it's 't' or 'f', it's a boolean
                    else if (currentChar == 't' || currentChar == 'f') {
                        value = jsonString.substring(i, i + 4).equals("true");
                        i += 4;
                    }
                    //if it's 'n', it's null
                    else if (currentChar == 'n') {
                        value = null;
                        i += 4;
                    }
                    //if it's an opening brace, it's an object
                    else if (currentChar == '{') {
                        JSON nestedJson = fromString(jsonString.substring(i));
                        value = nestedJson;
                        i += nestedJson.toString().length() + 1; //account for the closing brace
                    }
                }

                json.put(key, value);

                //skip spaces and handle closing brace
                while (i < jsonString.length() && Character.isWhitespace(jsonString.charAt(i))) {
                    i++;
                }

                if (i < jsonString.length() && jsonString.charAt(i) == '}') {
                    i++;
                    break;
                }

                //skip the comma
                if (i < jsonString.length() && jsonString.charAt(i) == ',') {
                    i++;
                }
            }
        }

        return json;
    }

    public String getString(String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; //return null if the value is not a String or does not exist
    }
}