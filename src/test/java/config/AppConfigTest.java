package config;

import com.github.vatbub.common.core.Common;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

public class AppConfigTest {
    @BeforeClass
    public static void oneTimeSetUp(){
        Common.resetInstance();
        Common.getInstance().setAppName("fokprojectUnitTests");
    }

    @Test
    public void loadRemoteConfigTest() throws IOException {
        AppConfig.reloadRemoteConfig();
        Assert.assertNotNull(AppConfig.getRemoteConfig());
    }

    @Test
    public void updateClassifierTest() {
        Common.getInstance().setMockPackaging("exe");
        Assert.assertEquals(AppConfig.getRemoteConfig().getValue("exeUpdateFileClassifier"), AppConfig.getUpdateFileClassifier());
        Common.getInstance().setMockPackaging("jar");

        Assert.assertEquals(AppConfig.getRemoteConfig().getValue("jarUpdateFileClassifier"), AppConfig.getUpdateFileClassifier());

        Common.getInstance().setMockPackaging(null);
        Assert.assertEquals(AppConfig.getRemoteConfig().getValue("jarUpdateFileClassifier"), AppConfig.getUpdateFileClassifier());
    }

    @Test
    public void supportedFOKConfigModelVersionTest(){
        Assert.assertNotNull(AppConfig.getSupportedFOKConfigModelVersion());
        Assert.assertTrue(AppConfig.getSupportedFOKConfigModelVersion().size()>0);
    }
}
