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
import config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class PrivateInstructorInstantiationTests {
    @Test
    public void instantiationTest() throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        List<Class> classesToTest = new ArrayList<>();
        classesToTest.add(AppConfig.class);
        classesToTest.add(AppListFile.FileFormat.class);
        classesToTest.add(ImportedAppListFile.FileFormat.class);
        classesToTest.add(LocalMetadataFile.FileFormat.class);
        classesToTest.add(MVNMetadataFile.FileFormat.class);
        classesToTest.add(MVNMetadataFile.SnapshotFileFormat.class);

        for (Class clazz : classesToTest) {
            try {
                //noinspection unchecked
                Constructor constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
                Assert.fail("IllegalStateException expected, class that failed: " + clazz.getName());
            } catch (IllegalStateException|InvocationTargetException e) {
                FOKLogger.log(PrivateInstructorInstantiationTests.class.getName(), Level.INFO, "Success: Unable to instantiate class " + clazz.getName() + " due to a " + e.getClass().getName(), e);
                Assert.assertTrue(getCauseList(e).containsThrowableOfType(IllegalStateException.class));
            }
        }
    }

    private static class ThrowableList extends LinkedList<Throwable>{
        /**
         * Constructs an empty list.
         */
        public ThrowableList() {
        }

        /**
         * Constructs a list containing the elements of the specified
         * collection, in the order they are returned by the collection's
         * iterator.
         *
         * @param c the collection whose elements are to be placed into this list
         * @throws NullPointerException if the specified collection is null
         */
        public ThrowableList(@NotNull Collection<? extends Throwable> c) {
            super(c);
        }

        public boolean containsThrowableOfType(Class<? extends Throwable> throwableType){
            for(Throwable throwable:this){
                if (throwable.getClass().equals(throwableType)){
                    return true;
                }
            }

            return false;
        }
    }

    private ThrowableList getCauseList(Throwable throwable){
        ThrowableList res = new ThrowableList();
        getCauseList(throwable, res);
        return res;
    }

    private void getCauseList(Throwable throwable, ThrowableList causes){
        causes.add(throwable);
        if (throwable.getCause()!=null){
            getCauseList(throwable.getCause(), causes);
        }
    }
}
