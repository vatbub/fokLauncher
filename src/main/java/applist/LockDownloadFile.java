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


import com.github.vatbub.common.core.logging.FOKLogger;
import com.github.vatbub.common.updater.Version;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;

public class LockDownloadFile {
    private App app;
    private Version versionToLock;
    private static final String LOCK_FILE_NAME_SUFFIX = "_lock";
    private static final String LOCK_FILE_NAME_EXTENSION = "lock";

    public LockDownloadFile(App app, Version versionToLock) {
        setApp(app);
        setVersionToLock(versionToLock);
    }

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public void lock() throws IOException {
        FileUtils.writeStringToFile(getLockFile(), "locked", StandardCharsets.UTF_8);
    }

    public void unlock() throws IOException {
        if (!isLocked()) return;
        try {
            Files.delete(getLockFile().toPath());
        } catch (NoSuchFileException e) {
            FOKLogger.log(getClass().getName(), Level.WARNING, "Unable to delete the lock file due to an exception", e);
        }
    }

    public boolean isLocked() {
        return getLockFile().exists();
    }

    public Version getVersionToLock() {
        return versionToLock;
    }

    public void setVersionToLock(Version versionToLock) {
        this.versionToLock = versionToLock;
    }

    public File getLockFile() {
        File outputFile = app.getOutputFile(getVersionToLock());
        File containingFolder = outputFile.getParentFile();
        return containingFolder.toPath().resolve(FilenameUtils.getBaseName(outputFile.getName()) + LOCK_FILE_NAME_SUFFIX + "." + LOCK_FILE_NAME_EXTENSION).toFile();
    }
}
