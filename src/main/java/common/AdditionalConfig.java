package common;

import logging.FOKLogger;

import java.util.ArrayList;
import java.util.List;

public class AdditionalConfig {
	@SuppressWarnings("unused")
	private static FOKLogger log = new FOKLogger(AdditionalConfig.class.getName());

	// Project setup
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
	public static List<String> getSupportedFOKConfigModelVersion() {
		List<String> res = new ArrayList<>();

		res.add("0.0.1");

		return res;
	}
}
