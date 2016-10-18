package common;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import logging.FOKLogger;

public class AdditionalConfig {
	private static FOKLogger log = new FOKLogger(AdditionalConfig.class.getName());

	// Project setup

	public static String getUpdateFileClassifier() {
		String packaging = Common.getPackaging();
		if (packaging != null) {
			if (packaging.equals("exe")) {
				return "";
			} else {
				return "";
			}
		} else {
			// no packaging found
			return "";
		}
	}

	// AppList

	public static List<String> getSupportedFOKConfigModelVersion() {
		List<String> res = new ArrayList<String>();

		res.add("0.0.1");

		return res;
	}
}
