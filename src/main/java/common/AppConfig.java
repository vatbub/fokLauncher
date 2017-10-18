package common;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 Frederik Kammel
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
import com.github.vatbub.common.core.logging.FOKLogger;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AppConfig {
    private AppConfig() {
        throw new IllegalStateException("Class may not be instantiated");
    }

    public static final String artifactID = "foklauncher";
    public static final String groupID = "com.github.vatbub";
    /**
     * The subfolder in foklaunchers appdata folder where the actual apps will
     * be downloaded to. {appName} will be replaced by the apps name.
     */
    public static final String subfolderToSaveApps = "apps" + File.separator + "{groupId}" + File.separator + "{artifactId}" + File.separator + "{classifier}";
    /**
     * The filename of the apps metadata file
     */
    public static final String appMetadataFileName = "metadata.xml";

    // AppList
    /**
     * The file name where the app list cache is saved
     */
    public static final String appListCacheFileName = "appListCache.xml";
    /**
     * The name of the xml file where the list of imported apps is saved.
     */
    public static final String importedAppListFileName = "importedApps.xml";

    // Project setup
    public static URL getUpdateRepoBaseURL() {
        URL res = null;
        try {
            res = new URL("https://dl.bintray.com/vatbub/fokprojectsReleases");
        } catch (MalformedURLException e) {
            FOKLogger.log(AppConfig.class.getName(), Level.SEVERE, "Malicious config", e);
        }

        return res;
    }

    public static String getUpdateFileClassifier() {
        String packaging = Common.getInstance().getPackaging();
        if (packaging != null) {
            if (packaging.equals("exe")) {
                return "WindowsExecutable";
            } else {
                return "jar-with-dependencies";
            }
        } else {
            // no packaging found
            return "jar-with-dependencies";
        }
    }

    public static URL getAppListXMLURL() {
        URL res = null;
        try {
            res = new URL("https://www.dropbox.com/s/muikjvx0x2tpetb/fokprojectsOnLauncher.xml?dl=1");
        } catch (MalformedURLException e) {
            FOKLogger.log(AppConfig.class.getName(), Level.SEVERE, "Malicious config", e);
        }

        return res;
    }

    public static List<String> getSupportedFOKConfigModelVersion() {
        List<String> res = new ArrayList<>();

        res.add("0.0.1");

        return res;
    }

    //MOTD
    @NotNull
    public static URL getMotdFeedUrl() {
        try {
            return new URL("https://fokprojects.mo-mar.de/message-of-the-day/feed/");
        } catch (MalformedURLException e) {
            FOKLogger.log(AppConfig.class.getName(), Level.SEVERE, "Malicious config", e);
            //noinspection ConstantConditions
            return null;
        }
    }
}
