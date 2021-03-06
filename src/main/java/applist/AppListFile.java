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


import com.github.vatbub.common.core.Common;
import config.AppConfig;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Represents the list of apps found on the server.
 */
public class AppListFile {
    private String modelVersion;
    private AppList appList;

    /**
     * Downloads the app list from the server configured in the app config.
     *
     * @param offlineMode If set to {@code true}, the cached app list will be returned
     * @throws JDOMException If the app list xml cannot be parsed
     * @throws IOException   If the app list cannot be downloaded for any reason or the computer is offline (or {@code offlineMode == true}) and no cached app list exists
     */
    public AppListFile(boolean offlineMode) throws JDOMException, IOException {
        this(new URL(AppConfig.getInstance().getRemoteConfig().getValue("appListXMLURL")), Common.getInstance().getAndCreateAppDataPath() + AppConfig.getInstance().getRemoteConfig().getValue("appListCacheFileName"), offlineMode);
    }

    /**
     * Downloads the app list from the specified URL.
     *
     * @param onlineListURL        The URL to download the list from
     * @param offlineCacheFileName The name of the offline cache file name
     * @param offlineMode          If set to {@code true}, the cached app list will be returned
     * @throws JDOMException If the app list xml cannot be parsed
     * @throws IOException   If the app list cannot be downloaded for any reason or the computer is offline (or {@code offlineMode == true}) and no cached app list exists
     */
    public AppListFile(URL onlineListURL, String offlineCacheFileName, boolean offlineMode) throws JDOMException, IOException {
        readFile(onlineListURL, offlineCacheFileName, offlineMode);
    }

    private Document getOnlineDocument(URL onlineListURL, String offlineCacheFileName) throws JDOMException, IOException {
        Document res = new SAXBuilder().build(onlineListURL);
        (new XMLOutputter(Format.getPrettyFormat())).output(res, new FileOutputStream(offlineCacheFileName));
        return res;
    }

    private Document getOfflineCache(String offlineCacheFileName) throws JDOMException, IOException {
        return new SAXBuilder().build(new File(offlineCacheFileName));
    }

    /**
     * Reads the specified app list file
     *
     * @param onlineListURL        The URL to download the list from
     * @param offlineCacheFileName The name of the offline cache file name
     * @throws JDOMException If the app list xml cannot be parsed
     * @throws IOException   If the app list cannot be downloaded for any reason
     */
    private void readFile(URL onlineListURL, String offlineCacheFileName, boolean offlineMode) throws JDOMException, IOException {
        Document onlineAppList;

        if (offlineMode) {
            onlineAppList = getOfflineCache(offlineCacheFileName);
        } else {
            try {
                onlineAppList = getOnlineDocument(onlineListURL, offlineCacheFileName);
            } catch (ConnectException | UnknownHostException e) {
                try {
                    onlineAppList = new SAXBuilder().build(new File(offlineCacheFileName));
                } catch (FileNotFoundException e1) {
                    throw new UnknownHostException("Could not connect to " + AppConfig.getInstance().getRemoteConfig().getValue("appListXMLURL")
                            + " and app list cache not found. \nPlease ensure a stable internet connection.");
                }
            }
        }
        Element fokLauncherElement = onlineAppList.getRootElement();
        setModelVersion(fokLauncherElement.getChild(FileFormat.MODEL_VERSION_TAG_NAME).getValue());

        // Check for unsupported modelVersion
        if (!AppConfig.getInstance().getSupportedFOKConfigModelVersion().contains(getModelVersion())) {
            throw new IllegalStateException(
                    "The modelVersion of the fokprojectsOnLauncher.xml file is not supported! (modelVersion is "
                            + modelVersion + ")");
        }

        setAppList(new AppList());

        for (Element app : fokLauncherElement.getChild(FileFormat.APP_LIST_TAG_NAME).getChildren(FileFormat.APP_TAG_NAME)) {
            MVNCoordinates mvnCoordinates = new MVNCoordinates(new URL(app.getChild(FileFormat.REPO_BASE_URL_TAG_NAME).getValue()),
                    new URL(app.getChild(FileFormat.SNAPSHOT_REPO_BASE_URL_TAG_NAME).getValue()), app.getChild(FileFormat.GROUP_ID_TAG_NAME).getValue(),
                    app.getChild(FileFormat.ARTIFACT_ID_TAG_NAME).getValue());
            App newApp = new App(app.getChild(FileFormat.APP_NAME_TAG_NAME).getValue(), mvnCoordinates);

            // Add classifier only if one is defined
            if (app.getChild(FileFormat.CLASSIFIER_TAG_NAME) != null) {
                newApp.getMvnCoordinates().setClassifier(app.getChild(FileFormat.CLASSIFIER_TAG_NAME).getValue());
            }

            if (app.getChild(FileFormat.ADDITIONAL_INFO_TAG_NAME) != null) {
                newApp.setAdditionalInfoURL(new URL(app.getChild(FileFormat.ADDITIONAL_INFO_TAG_NAME).getValue()));
            }

            if (app.getChild(FileFormat.CHANGELOG_URL_TAG_NAME) != null) {
                newApp.setChangelogURL(new URL(app.getChild(FileFormat.CHANGELOG_URL_TAG_NAME).getValue()));
            }

            getAppList().add(newApp);
        }
    }

    /**
     * Returns the model version of this file.
     *
     * @return The model version of this file.
     */
    public String getModelVersion() {
        return modelVersion;
    }

    /**
     * Sets the model version of this file.
     *
     * @param modelVersion The model version to set
     */
    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    /**
     * Gets the list of apps defined in this app list file.
     *
     * @return The list of apps defined in this app list file.
     */
    public AppList getAppList() {
        return appList;
    }

    /**
     * Sets the list of apps defined in this app list file.
     *
     * @param appList The list of apps to set
     */
    public void setAppList(AppList appList) {
        this.appList = appList;
    }

    /**
     * Describes the tag names used in an AppListFile
     */
    public static class FileFormat {
        public static final String MODEL_VERSION_TAG_NAME = "modelVersion";
        public static final String APP_LIST_TAG_NAME = "apps";
        public static final String APP_TAG_NAME = "app";
        public static final String REPO_BASE_URL_TAG_NAME = "repoBaseURL";
        public static final String SNAPSHOT_REPO_BASE_URL_TAG_NAME = "snapshotRepoBaseURL";
        public static final String GROUP_ID_TAG_NAME = "groupId";
        public static final String ARTIFACT_ID_TAG_NAME = "artifactId";
        public static final String APP_NAME_TAG_NAME = "name";
        public static final String CLASSIFIER_TAG_NAME = "classifier";
        public static final String ADDITIONAL_INFO_TAG_NAME = "additionalInfoURL";
        public static final String CHANGELOG_URL_TAG_NAME = "changelogURL";

        private FileFormat() {
            throw new IllegalStateException("Class may not be instantiated");
        }
    }
}
