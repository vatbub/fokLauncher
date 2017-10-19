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
import com.github.vatbub.common.core.logging.FOKLogger;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AppConfig {
    private static Config remoteConfig;

    static {
        try {
            remoteConfig = new Config(new URL("https://www.dropbox.com/s/i8gyyd6hcio23k9/foklauncherremoteconfig.properties?dl=1"), AppConfig.class.getResource("defaultConfig.properties"), "foklauncherConfigCache.properties");
        } catch (IOException e) {
            FOKLogger.log(AppConfig.class.getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
        }
    }

    public static String getUpdateFileClassifier() {
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

    public static List<String> getSupportedFOKConfigModelVersion() {
        List<String> res = new ArrayList<>();

        res.add("0.0.1");

        return res;
    }

    public static Config getRemoteConfig() {
        return remoteConfig;
    }
}
