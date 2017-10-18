package applist;

/*-
 * #%L
 * FOK Launcher
 * %%
 * Copyright (C) 2016 - 2017 Frederik Kammel
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


import com.github.vatbub.common.core.Common;
import com.github.vatbub.common.core.logging.FOKLogger;
import common.AppConfig;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

public class ImportedAppListFile {
    private AppList appList;
    private String fileName;

    public ImportedAppListFile() {
        this(Common.getInstance().getAndCreateAppDataPath() + AppConfig.importedAppListFileName);
    }

    public ImportedAppListFile(String fileName) {
        tryReadFile(fileName);
    }

    private void tryReadFile(String fileName) {
        this.fileName = fileName;
        Element root;
        Document appsDoc;
        Element modelVersion;
        Element appsElement;

        try {
            appsDoc = new SAXBuilder().build(fileName);
            root = appsDoc.getRootElement();

            modelVersion = root.getChild(FileFormat.MODEL_VERSION_TAG_NAME);
            appsElement = root.getChild(FileFormat.APP_LIST_TAG_NAME);

            // Check if one of those elements is not defined
            if (modelVersion == null) {
                throw new NullPointerException("modelVersion is null");
            } else if (appsElement == null) {
                throw new NullPointerException("appsElement is null");
            }

            setAppList(new AppList());

            for (Element app : appsElement.getChildren()) {
                getAppList().add(new App(new File(app.getChild(FileFormat.FILE_NAME_TAG_NAME).getValue())));
            }
        } catch (JDOMException | IOException | NullPointerException e) {
            FOKLogger.log(getClass().getName(), Level.SEVERE, FOKLogger.DEFAULT_ERROR_TEXT, e);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void saveFile() throws IOException {
        Element root = new Element(FileFormat.ROOT_NODE_NAME);
        Document appsDoc = new Document(root);

        Element modelVersion = new Element(FileFormat.MODEL_VERSION_TAG_NAME);
        Element appsElement = new Element(FileFormat.APP_LIST_TAG_NAME);

        root.addContent(modelVersion);
        root.addContent(appsElement);

        for (App app : getAppList()) {
            Element appElement = new Element(FileFormat.APP_TAG_NAME);
            Element fileNameElement = new Element(FileFormat.FILE_NAME_TAG_NAME);

            fileNameElement.setText(app.getImportFile().getAbsolutePath());
            appElement.addContent(fileNameElement);
            appsElement.addContent(appElement);
        }

        // Create directories if necessary
        File f = new File(getFileName());
        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();
        // Create empty file on disk if necessary
        (new XMLOutputter(Format.getPrettyFormat())).output(appsDoc, new FileOutputStream(fileName));
    }

    public AppList getAppList() {
        return appList;
    }

    public void setAppList(AppList appList) {
        this.appList = appList;
    }

    public class FileFormat {
        public static final String ROOT_NODE_NAME = "fokLauncher";
        public static final String MODEL_VERSION_TAG_NAME = "modelVersion";
        public static final String APP_LIST_TAG_NAME = "importedApps";
        public static final String APP_TAG_NAME = "app";
        public static final String FILE_NAME_TAG_NAME = "fileName";

        private FileFormat() {
            throw new IllegalStateException("Class may not be instantiated");
        }
    }
}
