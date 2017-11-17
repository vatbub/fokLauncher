package config;

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
import com.github.vatbub.common.core.Config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a wrapper for the launcher's remote config and additional config parameters.
 */
public class AppConfig {
    private static AppConfig instance;
    private Config remoteConfig;
    private List<String> supportedFOKConfigModelVersion = new ArrayList<>();

    public AppConfig() {
        supportedFOKConfigModelVersion.add("0.0.1");

        try {
            reloadRemoteConfig();
        } catch (IOException e) {
            // rethrow
            throw new IllegalStateException("Unable to parse the remote config URL", e);
        }
    }

    public static AppConfig getInstance() {
        if (instance==null){
            instance = new AppConfig();
        }
        return instance;
    }

    public static void resetInstance() {
        instance=null;
    }

    /**
     * Returns the list of supported model versions of foklauncher files
     *
     * @return The list of supported model versions of foklauncher files
     */
    public List<String> getSupportedFOKConfigModelVersion() {
        return supportedFOKConfigModelVersion;
    }

    /**
     * Returns the remote config.
     *
     * @return The remote config.
     */
    public Config getRemoteConfig() {
        return remoteConfig;
    }

    /**
     * Reloads the remote config
     *
     * @throws IOException If the remote config URL is malformed or the remote config cannot be loaded for any reason
     */
    public void reloadRemoteConfig() throws IOException {
        remoteConfig = new Config(new URL("https://www.dropbox.com/s/i8gyyd6hcio23k9/foklauncherremoteconfig.properties?dl=1"), AppConfig.class.getResource("defaultConfig.properties"), true, "foklauncherConfigCache.properties", true);
    }

    /**
     * The classifier to use for updates.
     *
     * @return The classifier to use for updates.
     */
    public String getUpdateFileClassifier() {
        String packaging = Common.getInstance().getPackaging();
        if (packaging != null) {
            if (packaging.equals("exe")) {
                return getRemoteConfig().getValue("exeUpdateFileClassifier");
            } else {
                return getRemoteConfig().getValue("jarUpdateFileClassifier");
            }
        } else {
            // no packaging found
            return getRemoteConfig().getValue("jarUpdateFileClassifier");
        }
    }
}
