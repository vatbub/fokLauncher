package extended;

import java.util.Locale;

public class GuiLanguage {

	private Locale locale;
	private String defaultLocaleText;
	private Locale currentDisplayLanguage;

	public GuiLanguage(Locale locale, String defaultLocale, Locale currentDisplayLanguage) {
		setLocale(locale);
		setDefaultLocale(defaultLocale);
		setCurrentDisplayLanguage(currentDisplayLanguage);
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
	public String getDefaultLocaleText() {
		return defaultLocaleText;
	}

	/**
	 * @param defaultLocaleText the defaultLocaleText to set
	 */
	public void setDefaultLocale(String defaultLocaleText) {
		this.defaultLocaleText = defaultLocaleText;
	}

	/**
	 * @return the currentDisplayLanguage
	 */
	public Locale getCurrentDisplayLanguage() {
		return currentDisplayLanguage;
	}

	/**
	 * @param currentDisplayLanguage the currentDisplayLanguage to set
	 */
	public void setCurrentDisplayLanguage(Locale currentDisplayLanguage) {
		this.currentDisplayLanguage = currentDisplayLanguage;
	}

	@Override
	public String toString() {
		if (locale.toLanguageTag().equals("und")) {
			return defaultLocaleText;
		} else {
			return this.getLocale().getDisplayLanguage(this.getCurrentDisplayLanguage()) + " (" + this.getLocale().getDisplayLanguage(Locale.ENGLISH)
					+ ")";
		}
	}

}
