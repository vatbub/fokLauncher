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
import com.github.vatbub.common.updater.Version;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;

public class LockDownloadFileTest {
    private App sampleApp = new App("sampleTestApp", new MVNCoordinates("com.github.vatbub", "sampleApp"));
    private Version versionToTest = new Version("0.0.1");

    @After
    public void cleanup() throws IOException {
        if (sampleApp.getLockFile(versionToTest).getLockFile().exists())
            Files.delete(sampleApp.getLockFile(versionToTest).getLockFile().toPath());
    }

    @Test
    public void lockTest() throws IOException {
        Common.resetInstance();
        Common.getInstance().setAppName("fokprojectUnitTests");

        sampleApp.getLockFile(versionToTest).lock();
        Assert.assertTrue(sampleApp.getLockFile(versionToTest).isLocked());

        sampleApp.getLockFile(versionToTest).unlock();
        Assert.assertFalse(sampleApp.getLockFile(versionToTest).isLocked());
    }
}
