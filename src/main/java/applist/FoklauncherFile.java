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

public class FoklauncherFile {
    private Properties properties;
    private File sourceFile;

    public FoklauncherFile(File fileToRead) throws IOException {
        sourceFile = fileToRead;
        readFile();
    }

    private void readFile() throws IOException {
        if (!getSourceFile().isFile()) {
            // Not a file
            throw new IOException("The specified file is not a file");
        } else if (!getSourceFile().canRead()) {
            // Cannot write to file
            throw new IOException("The specified file is read-only");
        }

        properties = new Properties();
        if (getSourceFile().exists()) {
            try (FileReader fileReader = new FileReader(getSourceFile())) {
                properties.load(fileReader);
            }
        }
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public String getValue(Property property) {
        return properties.getProperty(property.toString());
    }

    public String getValue(Property property, String defaultValue) {
        return properties.getProperty(property.toString(), defaultValue);
    }

    public void setValue(Property property, String value) {
        properties.setProperty(property.toString(), value);
    }

    public void save() throws IOException {
        try (FileOutputStream out = new FileOutputStream(getSourceFile())) {
            properties.store(out, "This file stores info about a java app. To open this file, get the foklauncher (http://github.com/vatbub/foklauncher/)");
        }
    }

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
