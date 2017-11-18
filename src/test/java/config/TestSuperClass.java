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


import applist.ImportedAppListFileTest;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.logging.Level;

public abstract class TestSuperClass {
    @Before
    public void superSetUp(){
        Common.resetInstance();
        AppConfig.resetInstance();
        Common.getInstance().setAppName("fokprojectUnitTests");
    }

    @After
    public void cleanAppData() {
        if (Common.getInstance().getAppDataPathAsFile().exists()) {
            try {
                FileUtils.deleteDirectory(Common.getInstance().getAppDataPathAsFile());
            } catch (IOException e) {
                FOKLogger.log(ImportedAppListFileTest.class.getName(), Level.INFO, "Unable to delete the test folder, ignoring that...", e);
            }
        }
    }
}
