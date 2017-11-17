package config;

import applist.ImportedAppListFileTest;
import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.logging.Level;

public class TestSuperClass {
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
