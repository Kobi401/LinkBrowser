package com.kobi401.browser.utils.fs;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * A flexible .Link file manager that supports:
 * - Loading and storing multiple data types (Strings, Integers, Doubles, Booleans)
 * - Backward compatibility with older Link Files (v1.0)
 * - Version checking and warnings
 *
 * TODO:
 * - Support for saving the loaded .link files to a LinkBrowser directory.
 * - Implement proper version migration for older .link files to ensure smooth transitions when upgrading file formats.
 * - Add functionality to detect and alert the user if a .link file is corrupted or in an unsupported format.
 * - Improve performance by implementing a caching mechanism to store frequently accessed data.
 * - Add more error handling for edge cases like missing files or invalid data types in the file.
 * - Allow for customizable file paths to load .link files from different locations.
 *
 * Ideas:
 * - Add support for nested data structures (e.g., Lists, Maps) to enhance the flexibility of the .link file format.
 * - Integrate with a GUI or command-line interface to allow users to modify .link files easily without editing them manually.
 * - Introduce an auto-backup system to prevent data loss when working with important .link files.
 * - Enable encryption support for .link files to ensure sensitive data is securely stored. (When a user system is added ill prob
 *      have it store the user data in a encrypted .link file)
 * - Provide a method to merge or combine multiple .link files into one, allowing for easier data sharing or migration.
 */


public class LinkFileManager {

    private static final String CURRENT_VERSION = "# Link File v1.1";

    //Version 1 support
    private static final String OLD_VERSION = "# Link File v1.0";

    /**
     * Loads a list of values or links from a file.
     * If the file is in v1.0 format, it loads only links as Strings.
     *
     * @param filePath The path to the file.
     * @return A list of loaded values (Strings, Integers, Doubles, Booleans).
     */
    public static List<Object> loadValuesFromFile(String filePath) {
        List<Object> values = new ArrayList<>();

        try {
            URL resourceUrl = LinkFileManager.class.getClassLoader().getResource(filePath);
            if (resourceUrl == null) {
                System.err.println("Resource file not found: " + filePath);
                return values;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceUrl.openStream()))) {
                String line;
                boolean versionChecked = false;
                boolean isLegacyLinkFile = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.isEmpty() || line.startsWith("#")) {
                        if (!versionChecked) {
                            versionChecked = true;
                            if (line.equals(OLD_VERSION)) {
                                isLegacyLinkFile = true; //this is a v1.0 link file
                                System.out.println("Loading legacy Link File v1.0...");
                            } else if (!line.equals(CURRENT_VERSION)) {
                                System.err.println("Warning: File version mismatch! Expected: " + CURRENT_VERSION +
                                        ", Found: " + line + ". This may cause issues.");
                            }
                        }
                        continue;
                    }

                    //if it's a legacy file, treat everything as a link (String)
                    if (isLegacyLinkFile) {
                        values.add(line);
                    } else {
                        //try to parse mixed data types
                        Object parsedValue = parseValue(line);
                        if (parsedValue != null) {
                            values.add(parsedValue);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return values;
    }

    /**
     * Saves a list of values or links to a file.
     *
     * @param filePath The path to the file.
     * @param values   The list of values to save.
     * @param saveAsLegacy If true, saves it as a v1.0 link file (only URLs).
     */
    public static void saveValuesToFile(String filePath, List<Object> values, boolean saveAsLegacy) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            writer.write(saveAsLegacy ? OLD_VERSION : CURRENT_VERSION); // Choose file version
            writer.newLine();

            for (Object value : values) {
                writer.write(value.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }

    /**
     * Parses a string value into its appropriate data type (Integer, Double, Boolean, or String).
     *
     * @param value The string to parse.
     * @return The parsed object, or null if it cannot be parsed.
     */
    private static Object parseValue(String value) {
        // Try parsing as Integer
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {}

        // Try parsing as Double
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {}

        // Try parsing as Boolean
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }

        // Default: return as String
        return value;
    }
}