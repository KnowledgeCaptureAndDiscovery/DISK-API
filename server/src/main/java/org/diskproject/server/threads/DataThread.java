package org.diskproject.server.threads;

public class DataThread implements Runnable {

    @Override
    public void run() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    // All of this needs to be reworked to clear caches and re run queries.
    /*public class DataMonitor implements Runnable {
        boolean stop;
        ScheduledFuture<?> scheduledFuture;

        public DataMonitor() {
            stop = false;
            scheduledFuture = monitor.scheduleWithFixedDelay(this, 0, 1, TimeUnit.DAYS);
        }

        public void run() {
            System.out.println("[D] Running data monitor thread");
            try {
                Thread.sleep(5000);
                if (stop) {
                    scheduledFuture.cancel(false);
                    while (!Thread.currentThread().isInterrupted()) {
                        Thread.currentThread().interrupt();
                    }
                } else if (!this.equals(dataThread)) {
                    stop();
                    return;
                } else {
                    // Re-run all hypothesis FIXME:
                    // runAllHypotheses("admin");
                }
            } catch (Exception e) {
                scheduledFuture.cancel(false);
                while (!Thread.interrupted()) {
                    stop = true;
                    Thread.currentThread().interrupt();
                }
            }

        }

        public void stop() {
            while (!Thread.interrupted()) {
                stop = true;
                scheduledFuture.cancel(false);
                Thread.currentThread().interrupt();
            }
        }
    }*/
    
}
