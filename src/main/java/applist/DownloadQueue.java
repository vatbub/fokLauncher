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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class DownloadQueue extends LinkedList<DownloadQueueEntry> {
    private final IntegerProperty currentQueueCount = new SimpleIntegerProperty();
    private final IntegerProperty currentTotalDownloadCount = new SimpleIntegerProperty();
    private volatile int parallelDownloadCount;
    private List<DownloadThread> threadPool = new LinkedList<>();

    public DownloadQueue(){
        this(2);
    }

    public DownloadQueue(int parallelDownloadCount){
        setParallelDownloadCount(parallelDownloadCount);
    }

    private synchronized void monitorThreadCount() {
        cleanThreadPoolUp();

        // add new threads if we are below the desired number of parallel downloads
        int parallelDownloadCountCopy = getParallelDownloadCount();
        while (threadPool.size() < parallelDownloadCountCopy) {
            DownloadThread thread = new DownloadThread(this);
            threadPool.add(thread);
            thread.start();
        }

        // shut threads down if we have too many
        int numberOfThreadsToShutDown = getNumberOfThreadsThatAreNotShuttingDown() - parallelDownloadCountCopy;
        if (numberOfThreadsToShutDown > 0) {
            int shutDownThreads = 0;
            for (DownloadThread downloadThread : threadPool) {
                if (!downloadThread.isShutdownAfterDownload()) {
                    downloadThread.setShutdownAfterDownload(true);
                    shutDownThreads++;
                    if (shutDownThreads >= numberOfThreadsToShutDown)
                        break;
                }
            }
        }
    }

    @Override
    public DownloadQueueEntry set(int index, DownloadQueueEntry element) {
        DownloadQueueEntry res = super.set(index, element);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry removeFirst() {
        DownloadQueueEntry res = super.removeFirst();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry poll() {
        DownloadQueueEntry res = super.poll();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pollFirst() {
        DownloadQueueEntry res = super.pollFirst();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pollLast() {
        DownloadQueueEntry res = super.pollLast();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry pop() {
        DownloadQueueEntry res = super.pop();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry remove() {
        DownloadQueueEntry res = super.remove();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry remove(int index) {
        DownloadQueueEntry res = super.remove(index);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean remove(Object o) {
        boolean res = super.remove(o);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean add(DownloadQueueEntry app) {
        boolean res = super.add(app);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public DownloadQueueEntry removeLast() {
        DownloadQueueEntry res = super.removeLast();
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean addAll(Collection<? extends DownloadQueueEntry> c) {
        boolean res = super.addAll(c);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends DownloadQueueEntry> c) {
        boolean res = super.addAll(index, c);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean offer(DownloadQueueEntry app) {
        boolean res = super.offer(app);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean offerFirst(DownloadQueueEntry app) {
        boolean res = super.offerFirst(app);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean offerLast(DownloadQueueEntry app) {
        boolean res = super.offerLast(app);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean res = super.removeAll(c);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        boolean res = super.removeFirstOccurrence(o);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        boolean res = super.removeLastOccurrence(o);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean removeIf(Predicate<? super DownloadQueueEntry> filter) {
        boolean res = super.removeIf(filter);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean res = super.retainAll(c);
        updateQueueCount();
        monitorThreadCount();
        return res;
    }

    @Override
    public void add(int index, DownloadQueueEntry element) {
        super.add(index, element);
        updateQueueCount();
        monitorThreadCount();
    }

    @Override
    public void addFirst(DownloadQueueEntry app) {
        super.addFirst(app);
        updateQueueCount();
        monitorThreadCount();
    }

    @Override
    public void addLast(DownloadQueueEntry app) {
        super.addLast(app);
        updateQueueCount();
        monitorThreadCount();
    }

    @Override
    public void clear() {
        super.clear();
        updateQueueCount();
        monitorThreadCount();
    }

    @Override
    public void push(DownloadQueueEntry app) {
        super.push(app);
        updateQueueCount();
        monitorThreadCount();
    }

    synchronized void updateQueueCount() {
        currentQueueCount.set(size());
        currentTotalDownloadCount.set(size()+getNumberOfCurrentlyRunningDownloads());
    }

    public int getNumberOfCurrentlyRunningDownloads(){
        int res=0;
        for(DownloadThread downloadThread:threadPool){
            if (downloadThread.isBusy())
                res++;
        }
        return res;
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
        return parallelDownloadCount;
    }

    public void setParallelDownloadCount(int parallelDownloadCount) {
        this.parallelDownloadCount = parallelDownloadCount;
        monitorThreadCount();
    }

    private synchronized void cleanThreadPoolUp() {
        List<DownloadThread> threadPoolCopy = new LinkedList<>(threadPool);
        for (DownloadThread downloadThread : threadPool) {
            if (!downloadThread.isAlive())
                threadPoolCopy.remove(downloadThread);
        }
        threadPool = threadPoolCopy;
    }

    private int getNumberOfThreadsThatAreNotShuttingDown() {
        int res = 0;
        for (DownloadThread downloadThread : threadPool) {
            if (!downloadThread.isShutdownAfterDownload())
                res++;
        }
        return res;
    }

    public int getCurrentTotalDownloadCount() {
        return currentTotalDownloadCount.get();
    }

    public IntegerProperty currentTotalDownloadCountProperty() {
        return currentTotalDownloadCount;
    }
}
