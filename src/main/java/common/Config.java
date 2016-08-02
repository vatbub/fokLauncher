package common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Config {
	// Project setup
	public static URL getUpdateRepoBaseURL() {
		URL res = null;
		try {
			res = new URL("http://dl.bintray.com/vatbub/fokprojectsSnapshots");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}

	public static String artifactID = "foklauncher";
	public static String groupID = "fokprojects";
	public static String updateFileClassifier = "jar-with-dependencies";
	
	//AppList
	
	public static URL getAppListXMLURL() {
		URL res = null;
		try {
			res = new URL("https://www.dropbox.com/s/muikjvx0x2tpetb/fokprojectsOnLauncher.xml?dl=1");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}
	
	public static List<String> getSupportedFOKConfigModelVersion(){
		List<String> res = new ArrayList<String>();
		
		res.add("0.0.1");
		
		return res;
	}
	
	/**
	 * The subfolder in foklaunchers appdata folder where the actual apps will be downloaded to. {appName} will be replaced by the apps name.
	 */
	public static String subfolderToSaveApps = "apps" + File.separator + "{appName}";
	
	/**
	 * The filename of the apps metadata file
	 */
	public static String appMetadataFileName = "metadata.xml";
}