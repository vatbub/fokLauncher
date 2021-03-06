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

/**
 * A simple class that represents GUI languages
 */
public class GuiLanguage {

    private Locale locale;
    private String defaultLocaleText;

    /**
     * Creates a new instance.
     * @param locale The java {@code Locale}-object that represents the desired language
     * @param defaultLocale The text that should be used when no default gui language was defined by the user.
     */
    public GuiLanguage(Locale locale, String defaultLocale) {
        setLocale(locale);
        setDefaultLocale(defaultLocale);
    }

    /**
     * Returns the java locale of this GUILanguage
     * @return The java locale of this GUILanguage
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the java locale of this GUILanguage
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * Returns the text that should be used when no default gui language was defined by the user.
     * @return The text that should be used when no default gui language was defined by the user.
     */
    public String getDefaultLocaleText() {
        return defaultLocaleText;
    }

    /**
     * Sets the text that should be used when no default gui language was defined by the user.
     * @param defaultLocaleText The text that should be used when no default gui language was defined by the user.
     */
    public void setDefaultLocale(String defaultLocaleText) {
        this.defaultLocaleText = defaultLocaleText;
    }

    @Override
    public String toString() {
        if (locale.toLanguageTag().equals("und")) {
            return getDefaultLocaleText();
        } else {
            return this.getLocale().getDisplayLanguage(Locale.getDefault()) + " (" + this.getLocale().getDisplayLanguage(Locale.ENGLISH)
                    + ")";
        }
    }

}
