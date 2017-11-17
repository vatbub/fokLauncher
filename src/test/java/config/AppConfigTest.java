package config;

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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class AppConfigTest extends TestSuperClass{

    @Test
    public void loadRemoteConfigTest() throws IOException {
        AppConfig.getInstance().reloadRemoteConfig();
        Assert.assertNotNull(AppConfig.getInstance().getRemoteConfig());
    }

    @Test
    public void updateClassifierTest() {
        Common.getInstance().setMockPackaging("exe");
        Assert.assertEquals(AppConfig.getInstance().getRemoteConfig().getValue("exeUpdateFileClassifier"), AppConfig.getInstance().getUpdateFileClassifier());
        Common.getInstance().setMockPackaging("jar");

        Assert.assertEquals(AppConfig.getInstance().getRemoteConfig().getValue("jarUpdateFileClassifier"), AppConfig.getInstance().getUpdateFileClassifier());

        Common.getInstance().setMockPackaging(null);
        Assert.assertEquals(AppConfig.getInstance().getRemoteConfig().getValue("jarUpdateFileClassifier"), AppConfig.getInstance().getUpdateFileClassifier());
    }

    @Test
    public void supportedFOKConfigModelVersionTest(){
        Assert.assertNotNull(AppConfig.getInstance().getSupportedFOKConfigModelVersion());
        Assert.assertTrue(AppConfig.getInstance().getSupportedFOKConfigModelVersion().size()>0);
    }
}
