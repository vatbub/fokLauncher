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


import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * In-memory representation of a local metadata file of an app.
 */
public class LocalMetadataFile {
    private MVNCoordinates mvnCoordinates;
    private VersionList versionList;

    /**
     * Creates a new empty instance.
     */
    public LocalMetadataFile() {

    }

    /**
     * Reads the local metadata file from the specified location.
     *
     * @param fileToRead The file to read.
     * @throws JDOMException If the xml in the file cannot be parsed for any reason.
     * @throws IOException   If the file cannot be read for any reason
     */
    public LocalMetadataFile(File fileToRead) throws JDOMException, IOException {
        readFile(fileToRead);
    }

    private void readFile(File fileToRead) throws JDOMException, IOException {
        Document document = new SAXBuilder().build(fileToRead);
        VersionList res = new VersionList();

        setMvnCoordinates(new MVNCoordinates(null, null, document.getRootElement().getChild(FileFormat.GROUP_ID_TAG_NAME).getValue(), document.getRootElement().getChild(FileFormat.ARTIFACT_ID_TAG_NAME).getValue()));

        for (Element versionElement : document.getRootElement().getChild(FileFormat.VERSION_LIST_TAG_NAME).getChildren()) {
            res.add(new Version(versionElement.getChild(FileFormat.VERSION_TAG_NAME).getValue(),
                    versionElement.getChild(FileFormat.BUILD_NUMBER_TAG_NAME).getValue(), versionElement.getChild(FileFormat.TIMESTAMP_TAG_NAME).getValue()));
        }

        setVersionList(res);
    }

    /**
     * Returns the maven coordinates of the app specified in this file
     * @return The maven coordinates of the app specified in this file
     */
    public MVNCoordinates getMvnCoordinates() {
        return mvnCoordinates;
    }

    /**
     * Sets the maven coordinates of the app specified in this file
     * @param mvnCoordinates The maven coordinates to set
     */
    public void setMvnCoordinates(MVNCoordinates mvnCoordinates) {
        this.mvnCoordinates = mvnCoordinates;
    }

    /**
     * Returns the list of installed versions specified in this file.
     * @return The list of installed versions specified in this file.
     */
    public VersionList getVersionList() {
        return versionList;
    }

    /**
     * Sets the list of installed versions specified in this file.
     * @param versionList The list to set
     */
    public void setVersionList(VersionList versionList) {
        this.versionList = versionList;
    }

    /**
     * Saves this file at the specified location.
     * @param fileToSaveTo The location to save the file to.
     * @throws IOException If the file cannot be written for any reason
     */
    public void saveFile(File fileToSaveTo) throws IOException {
        Element artifactInfo = new Element(FileFormat.ROOT_NODE_TAG_NAME);
        Document versionDoc = new Document(artifactInfo);

        Element groupId = new Element(FileFormat.GROUP_ID_TAG_NAME);
        Element artifactId = new Element(FileFormat.ARTIFACT_ID_TAG_NAME);

        groupId.setText(getMvnCoordinates().getGroupId());
        artifactId.setText(getMvnCoordinates().getArtifactId());

        artifactInfo.addContent(groupId);
        artifactInfo.addContent(artifactId);

        Element versions = new Element(FileFormat.VERSION_LIST_TAG_NAME);
        artifactInfo.addContent(versions);

        for (Version version : getVersionList()) {
            Element versionElement = new Element(FileFormat.VERSION_OBJECT_TAG_NAME);

            Element versionNumber = new Element(FileFormat.VERSION_TAG_NAME);
            Element buildNumber = new Element(FileFormat.BUILD_NUMBER_TAG_NAME);
            Element timestamp = new Element(FileFormat.TIMESTAMP_TAG_NAME);

            versionNumber.setText(version.getVersion());
            buildNumber.setText(version.getBuildNumber());
            timestamp.setText(version.getTimestamp());

            versionElement.addContent(versionNumber);
            versionElement.addContent(buildNumber);
            versionElement.addContent(timestamp);

            versions.addContent(versionElement);
        }

        // Write xml-File
        // Create directories if necessary
        //noinspection ResultOfMethodCallIgnored
        fileToSaveTo.getParentFile().mkdirs();
        // Create empty file on disk if necessary
        (new XMLOutputter(Format.getPrettyFormat())).output(versionDoc, new FileOutputStream(fileToSaveTo));
    }

    /**
     * Descries the file format of a local metadata file
     */
    public static class FileFormat {
        private FileFormat() {
            throw new IllegalStateException("Class may not be instantiated");
        }

        public static final String ROOT_NODE_TAG_NAME = "artifactInfo";
        public static final String GROUP_ID_TAG_NAME = "groupId";
        public static final String ARTIFACT_ID_TAG_NAME = "artifactId";
        public static final String VERSION_LIST_TAG_NAME = "versions";
        public static final String VERSION_TAG_NAME = "version";
        public static final String VERSION_OBJECT_TAG_NAME = "version";
        public static final String BUILD_NUMBER_TAG_NAME = "buildNumber";
        public static final String TIMESTAMP_TAG_NAME = "timestamp";
    }
}
