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


import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class DownloadQueue extends LinkedList<DownloadQueueEntry> {
    private final IntegerProperty currentQueueCount = new SimpleIntegerProperty();
    private AtomicInteger parallelDownloadCount = new AtomicInteger(2);
    private ArrayList<Thread> threadPool = new ArrayList<>();

    @Override
    public DownloadQueueEntry set(int index, DownloadQueueEntry element) {
        DownloadQueueEntry res = super.set(index, element);
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry removeFirst() {
        DownloadQueueEntry res = super.removeFirst();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry poll() {
        DownloadQueueEntry res = super.poll();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pollFirst() {
        DownloadQueueEntry res = super.pollFirst();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pollLast() {
        DownloadQueueEntry res = super.pollLast();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pop() {
        DownloadQueueEntry res = super.pop();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry remove() {
        DownloadQueueEntry res = super.remove();
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry remove(int index) {
        DownloadQueueEntry res = super.remove(index);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean remove(Object o) {
        boolean res = super.remove(o);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean add(DownloadQueueEntry app) {
        boolean res = super.add(app);
        updateQueueCount();
        return res;
    }

    @Override
    public DownloadQueueEntry removeLast() {
        DownloadQueueEntry res = super.removeLast();
        updateQueueCount();
        return res;
    }

    @Override
    public boolean addAll(Collection<? extends DownloadQueueEntry> c) {
        boolean res = super.addAll(c);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends DownloadQueueEntry> c) {
        boolean res = super.addAll(index, c);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean offer(DownloadQueueEntry app) {
        boolean res = super.offer(app);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean offerFirst(DownloadQueueEntry app) {
        boolean res = super.offerFirst(app);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean offerLast(DownloadQueueEntry app) {
        boolean res = super.offerLast(app);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean res = super.removeAll(c);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        boolean res = super.removeFirstOccurrence(o);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        boolean res = super.removeLastOccurrence(o);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean removeIf(Predicate<? super DownloadQueueEntry> filter) {
        boolean res = super.removeIf(filter);
        updateQueueCount();
        return res;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean res = super.retainAll(c);
        updateQueueCount();
        return res;
    }

    @Override
    public void add(int index, DownloadQueueEntry element) {
        super.add(index, element);
        updateQueueCount();
    }

    @Override
    public void addFirst(DownloadQueueEntry app) {
        super.addFirst(app);
        updateQueueCount();
    }

    @Override
    public void addLast(DownloadQueueEntry app) {
        super.addLast(app);
        updateQueueCount();
    }

    @Override
    public void clear() {
        super.clear();
        updateQueueCount();
    }

    @Override
    public void push(DownloadQueueEntry app) {
        super.push(app);
        updateQueueCount();
    }


    private void updateQueueCount() {
        currentQueueCount.set(size());
    }

    public int getCurrentQueueCount() {
        synchronized (currentQueueCount) {
            return currentQueueCount.get();
        }
    }

    public ReadOnlyIntegerProperty currentQueueCountProperty() {
        return currentQueueCount;
    }

    public int getParallelDownloadCount() {
        return parallelDownloadCount.intValue();
    }

    public void setParallelDownloadCount(int parallelDownloadCount) {
        this.parallelDownloadCount.set(parallelDownloadCount);
    }
}
