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

import java.io.IOException;
import java.net.URL;

public class MVNSnapshotMetadataFile extends MVNMetadataFile {
    private Version snapshotVersionToDownload;

    public MVNSnapshotMetadataFile(MVNCoordinates mvnCoordinates, Version snapshotVersionToDownload) throws JDOMException, IOException {
        super(mvnCoordinates);
        if (!snapshotVersionToDownload.isSnapshot()) {
            throw new IllegalStateException(
                    "Latest version in this repository is a release and not a snapshot. This might happen if you host snapshots and releases in the same repository (which is not recommended). If you still need this case to be covered, please submit an issue at https://github.com/vatbub/fokLauncher/issues");
        }
        setSnapshotVersionToDownload(snapshotVersionToDownload);
        getFile(true);
    }

    @Override
    VersionList readVersionList(Element versioningElement) {
        VersionList res = new VersionList();
        Element latestSnapshot = versioningElement.getChild(FileFormat.LATEST_SNAPSHOT_TAG_NAME);

        Version version = getLatest();
        version.setBuildNumber(latestSnapshot.getChild(FileFormat.BUILD_NUMBER_TAG_NAME).getValue());

        // get the build timestamp
        version.setTimestamp(latestSnapshot.getChild(FileFormat.TIMESTAMP_TAG_NAME).getValue());

        res.add(version);
        return res;
    }

    @Override
    Document getMavenMetadata(boolean snapshotsEnabled) throws JDOMException, IOException {
        if (!snapshotsEnabled) {
            throw new IllegalArgumentException();
        }

        return new SAXBuilder()
                .build(new URL(this.getMvnCoordinates().getSnapshotRepoBaseURL().toString() + "/" + getMvnCoordinates().getGroupId().replace('.', '/')
                        + "/" + getMvnCoordinates().getArtifactId() + "/" + getSnapshotVersionToDownload().getVersion() + "/maven-metadata.xml"));
    }

    public Version getSnapshotVersionToDownload() {
        return snapshotVersionToDownload;
    }

    public void setSnapshotVersionToDownload(Version snapshotVersionToDownload) {
        this.snapshotVersionToDownload = snapshotVersionToDownload;
    }

    public class FileFormat {
        public static final String LATEST_SNAPSHOT_TAG_NAME = "snapshot";
        public static final String TIMESTAMP_TAG_NAME = "timestamp";
        public static final String BUILD_NUMBER_TAG_NAME = "buildNumber";
    }
}
