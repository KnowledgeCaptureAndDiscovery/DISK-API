package org.diskproject.server.threads;

import org.diskproject.server.repository.DiskRepository;

public class DataThread implements Runnable {
    DiskRepository disk;
    public DataThread(DiskRepository disk) {
        this.disk = disk;
    }

    @Override
    public void run() {
        System.out.println("[D] Running data monitor thread");
        disk.queryAllGoals();
    }

    public void stop() {
        while (!Thread.interrupted()) {
            Thread.currentThread().interrupt();
        }
    }
}
