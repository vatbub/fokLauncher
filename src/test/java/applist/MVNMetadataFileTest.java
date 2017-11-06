package applist;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.vatbub.common.updater.Version;
import com.github.vatbub.common.updater.VersionList;
import org.jdom2.JDOMException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class MVNMetadataFileTest {
    private final static String groupId = "com.github.vatbub";
    private final static String artifactId = "testArtifact";
    private final static String repoName = "repo";
    private final static String snapshotRepoName = "repo";
    private final static String lastUpdated = "20171028140208";
    private final static int port = 8089;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(port);

    private String getStubURLString(boolean snapshotRepo) {
        StringBuilder res = new StringBuilder("/");
        if (snapshotRepo) {
            res.append(snapshotRepoName);
        } else {
            res.append(repoName);
        }
        res.append("/").append(groupId.replace('.', '/')).append("/").append(artifactId).append("/maven-metadata.xml");
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
        if (versions.containsSnapshot()) {
            throw new IllegalArgumentException("version list may not contain snapshots");
        }

        Version latestVersion = Collections.max(versions);

        StringBuilder res = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<metadata>\n")
                .append("  <groupId>").append(groupId).append("</groupId>\n")
                .append("  <artifactId>").append(artifactId).append("</artifactId>\n")
                .append("  <versioning>\n")
                .append("    <latest>").append(latestVersion.getVersion()).append("</latest>\n")
                .append("    <release>").append(latestVersion).append("</release>\n")
                .append("    <versions>\n");

        for (Version version : versions) {
            res.append("      <version>").append(version.getVersion()).append("</version>\n");
        }

        res.append("    </versions>\n")
                .append("    <lastUpdated>").append(lastUpdated).append("</lastUpdated>\n")
                .append("  </versioning>\n")
                .append("</metadata>");
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

        for(Version version:mvnMetadataFile.getVersionList()){
            Assert.assertTrue(versions.contains(version));
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        Assert.assertEquals(LocalDateTime.from(f.parse(lastUpdated)), mvnMetadataFile.getLastUpdated());
    }
}
