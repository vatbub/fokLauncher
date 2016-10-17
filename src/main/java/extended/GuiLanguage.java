package extended;

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
	public String getDefaultLocaleText() {
		return defaultLocaleText;
	}

	/**
	 * @param defaultLocaleText the defaultLocaleText to set
	 */
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
