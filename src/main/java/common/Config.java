package common;

import java.net.MalformedURLException;
import java.net.URL;

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
}
