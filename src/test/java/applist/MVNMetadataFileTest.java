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


import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import config.TestSuperClass;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MVNMetadataFileTest extends TestSuperClass {
    private final static String groupId = "com.github.vatbub";
    private final static String artifactId = "testArtifact";
    private final static String repoName = "repo";
    private final static String snapshotRepoName = "repo";
    private final static String lastUpdated = "20171028140208";
    private final static int port = 8089;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    private String getStubURLString(boolean snapshotRepo) {
        return getStubURLString(snapshotRepo, null);
    }

    private String getStubURLString(boolean snapshotRepo, Version snapshotVersion) {
        StringBuilder res = new StringBuilder("/");
        if (snapshotRepo) {
            res.append(snapshotRepoName);
        } else {
            res.append(repoName);
        }
        res.append("/").append(groupId.replace('.', '/')).append("/").append(artifactId);

        if (snapshotVersion != null) {
            res.append("/").append(snapshotVersion.getVersion());
        }

        res.append("/maven-metadata.xml");
        return res.toString();
    }

    private URL getRepoURL(boolean snapshotRepo) throws MalformedURLException {
        StringBuilder res = new StringBuilder("http://localhost:").append(port).append("/");
        if (snapshotRepo) {
            res.append(snapshotRepoName);
        } else {
            res.append(repoName);
        }
        return new URL(res.toString());
    }

    private String getRepoMetadataContent(VersionList versions) {
        VersionList effectiveVersionList = new VersionList(versions.size());
        for (Version version : versions) {
            boolean alreadyContained = false;
            for (Version containedVersion : effectiveVersionList) {
                if (containedVersion.getVersion().equals(version.getVersion())) {
                    alreadyContained = true;
                    break;
                }
            }

            if (!alreadyContained) {
                effectiveVersionList.add(version);
            }
        }

        Version latestVersion = Collections.max(versions);
        Version latestRelease = null;
        if (versions.containsRelease()) {
            VersionList releasesOnly = versions.clone();
            releasesOnly.removeSnapshots();
            latestRelease = Collections.max(releasesOnly);
        }

        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<metadata>\n")
                .append("  <groupId>").append(groupId).append("</groupId>\n")
                .append("  <artifactId>").append(artifactId).append("</artifactId>\n")
                .append("  <versioning>\n")
                .append("    <latest>").append(latestVersion.getVersion()).append("</latest>\n");

        if (latestRelease != null) {
            res.append("    <release>").append(latestRelease).append("</release>\n");
        }

        res.append("    <versions>\n");

        for (Version version : effectiveVersionList) {
            res.append("      <version>").append(version.getVersion()).append("</version>\n");
        }

        res.append("    </versions>\n")
                .append("    <lastUpdated>").append(lastUpdated).append("</lastUpdated>\n")
                .append("  </versioning>\n")
                .append("</metadata>");
        return res.toString();
    }

    private String getSnaphotMetadataContent(Version snapshotVersion, List<String> extensionTypes) {
        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<metadata modelVersion=\"1.1.0\">\n")
                .append("  <groupId>").append(groupId).append("</groupId>\n")
                .append("  <artifactId>").append(groupId).append("</artifactId>\n")
                .append("  <version>").append(snapshotVersion.getVersion()).append("</version>\n")
                .append("  <versioning>\n")
                .append("    <snapshot>\n")
                .append("      <timestamp>").append(snapshotVersion.getTimestamp()).append("</timestamp>\n")
                .append("      <buildNumber>").append(snapshotVersion.getBuildNumber()).append("</buildNumber>\n")
                .append("    </snapshot>\n")
                .append("    <lastUpdated>").append(lastUpdated).append("</lastUpdated>\n")
                .append("    <snapshotVersions>\n");

        for (String extension : extensionTypes) {
            res.append("      <snapshotVersion>\n")
                    .append("        <extension>").append(extension).append("</extension>\n")
                    .append("        <value>").append(snapshotVersion.toString(true)).append("</value>\n")
                    .append("        <updated>").append(lastUpdated).append("</updated>\n")
                    .append("      </snapshotVersion>\n");
        }

        res.append("    </snapshotVersions>\n")
                .append("  </versioning>\n")
                .append("</metadata>\n");
        return res.toString();
    }

    @Test
    public void repoTest() throws IOException, JDOMException {
        VersionList versions = new VersionList();
        versions.add(new Version("0.0.2"));
        versions.add(new Version("1.0.2"));
        versions.add(new Version("1.0.3"));
        versions.add(new Version("1.0.3.1"));
        versions.add(new Version("1.0.4"));
        Version latest = Collections.max(versions);

        stubFor(get(urlEqualTo(getStubURLString(false)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(getRepoMetadataContent(versions))));

        MVNCoordinates mvnCoordinates = new MVNCoordinates(getRepoURL(false), getRepoURL(true), groupId, artifactId);
        MVNMetadataFile mvnMetadataFile = new MVNMetadataFile(mvnCoordinates, false);

        Assert.assertEquals(mvnCoordinates, mvnMetadataFile.getMvnCoordinates());
        Assert.assertEquals(latest, mvnMetadataFile.getLatest());
        Assert.assertEquals(latest, mvnMetadataFile.getLatestRelease());

        for (Version version : mvnMetadataFile.getVersionList()) {
            Assert.assertTrue(versions.contains(version));
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        Assert.assertEquals(LocalDateTime.from(f.parse(lastUpdated)), mvnMetadataFile.getLastUpdated());
    }

    @Test
    public void snapshotRepoTest() throws JDOMException, IOException {
        VersionList versions = new VersionList(1);
        Version version = new Version("0.0.3-SNAPSHOT", "1", "20161106.232243");
        versions.add(version);
        List<String> extensions = new ArrayList<>(3);
        extensions.add("jar");
        extensions.add("pom");

        // main metadata
        stubFor(get(urlEqualTo(getStubURLString(true)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(getRepoMetadataContent(versions))));

        // snapshot metadata
        stubFor(get(urlEqualTo(getStubURLString(true, version)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/xml")
                        .withBody(getSnaphotMetadataContent(version, extensions))));

        MVNCoordinates mvnCoordinates = new MVNCoordinates(getRepoURL(false), getRepoURL(true), groupId, artifactId);
        MVNMetadataFile mvnMetadataFile = new MVNMetadataFile(mvnCoordinates, true);

        Assert.assertEquals(mvnCoordinates, mvnMetadataFile.getMvnCoordinates());
        Assert.assertEquals(version, mvnMetadataFile.getLatest());
        Assert.assertNull(mvnMetadataFile.getLatestRelease());

        for (Version versionToCheck : mvnMetadataFile.getVersionList()) {
            Assert.assertEquals(version, versionToCheck);
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        Assert.assertEquals(LocalDateTime.from(f.parse(lastUpdated)), mvnMetadataFile.getLastUpdated());
    }
}
