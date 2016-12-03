package extended;

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


import java.util.Locale;

public class GuiLanguage {

	private Locale locale;
	private String defaultLocaleText;

	public GuiLanguage(Locale locale, String defaultLocale) {
		setLocale(locale);
		setDefaultLocale(defaultLocale);
	}

	/**
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * @param locale
	 *            the locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * @return the defaultLocaleText
	 */
	@SuppressWarnings("unused")
	public String getDefaultLocaleText() {
		return defaultLocaleText;
	}

	/**
	 * @param defaultLocaleText the defaultLocaleText to set
	 */
	@SuppressWarnings("unused")
	public void setDefaultLocale(String defaultLocaleText) {
		this.defaultLocaleText = defaultLocaleText;
	}

	@Override
	public String toString() {
		if (locale.toLanguageTag().equals("und")) {
			return defaultLocaleText;
		} else {
			return this.getLocale().getDisplayLanguage(Locale.getDefault()) + " (" + this.getLocale().getDisplayLanguage(Locale.ENGLISH)
					+ ")";
		}
	}

}
