package applist;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 - 2017 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Represents a file that contains metadata about an imported app.
 */
public class FoklauncherFile {
    private Properties properties;
    private File sourceFile;

    /**
     * Creates a new in-memory representation of the specified foklauncher file.
     * If the specified file does not exist, the object will act as if the specified file was an empty text file.
     *
     * @param fileToRead The file to read.
     * @throws IOException If the file cannot be read for any reason.
     */
    public FoklauncherFile(File fileToRead) throws IOException {
        sourceFile = fileToRead;
        readFile();
    }

    private void readFile() throws IOException {
        properties = new Properties();
        if (getSourceFile().exists()) {
            if (!getSourceFile().isFile()) {
                // Not a file
                throw new IOException("The specified file is not a file");
            } else if (!getSourceFile().canRead()) {
                // Cannot read to file
                throw new IOException("The specified file is read-only");
            }
            try (FileReader fileReader = new FileReader(getSourceFile())) {
                properties.load(fileReader);
            }
        }
    }

    /**
     * The source file of this object.
     *
     * @return The source file of this object.
     */
    public File getSourceFile() {
        return sourceFile;
    }

    /**
     * Returns the value of the specified property read from this file.
     *
     * @param property The property to read.
     * @return The value of the specified property read from this file or {@code null} if the property is not defined in this file.
     * @see #getValue(Property, String)
     */
    public String getValue(Property property) {
        return properties.getProperty(property.toString());
    }

    /**
     * Same as {@link #getValue(Property)} but returns the default value if the property is not defined in this file
     *
     * @param property     The property to read.
     * @param defaultValue The default value to be returned if the property is not defined in this file
     * @return The value of the specified property read from this file or {@code defaultValue} if the property is not defined in this file.
     * @see #getValue(Property)
     */
    public String getValue(Property property, String defaultValue) {
        return properties.getProperty(property.toString(), defaultValue);
    }

    /**
     * Sets a value in this file.
     *
     * @param property The property to set.
     * @param value    The value to set for this property.
     */
    public void setValue(Property property, String value) {
        properties.setProperty(property.toString(), value);
    }

    /**
     * Saves the file at the location specified by {@link #getSourceFile()}. If a file is already present at this location, it will be overwritten.
     *
     * @throws IOException If the file cannot be written for any reason
     */
    public void save() throws IOException {
        try (FileOutputStream out = new FileOutputStream(getSourceFile())) {
            properties.store(out, "This file stores info about a java app. To open this file, get the foklauncher (http://github.com/vatbub/foklauncher/)");
        }
    }

    /**
     * Properties that can be used in FOKLauncher files.
     */
    public enum Property {
        NAME("name"),
        REPO_BASE_URL("repoBaseURL"),
        SNAPSHOT_BASE_URL("snapshotRepoBaseURL"),
        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        CLASSIFIER("classifier"),
        ADDITIONAL_INFO_URL("additionalInfoURL"),
        CHANGELOG_URL("changelogURL");

        private final String text;

        Property(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
