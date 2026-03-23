package org.example;

public class Main {
    static final int THREAD_COUNT = 5;
    static final int TASK_DURATION_SECONDS = 9;
    static final int BAR_WIDTH = 30;

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < THREAD_COUNT; i++)
            System.out.println();

        Thread[] threads = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i;
            threads[i] = new Thread(() -> simulateTask(threadId, TASK_DURATION_SECONDS));
            threads[i].start();
        }

        for (Thread t : threads)
            t.join();
    }

    static void simulateTask(int task_id, int duration) {
        long threadId = Thread.currentThread().threadId();
        long startTime = System.currentTimeMillis();

        updateProgress(task_id, threadId, 0, -1);
        try {
            long delay = (long) (Math.random() * 5000);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        for (int j = 0; j < duration; j++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            updateProgress(task_id, threadId, (j + 1) * 100 / duration, -1);
        }

        long elapsed = System.currentTimeMillis() - startTime;
        updateProgress(task_id, threadId, 100, elapsed);
    }

    static synchronized void updateProgress(int taskId, long threadId, int progress, long elapsedMs) {
        int linesUp = THREAD_COUNT - taskId;
        System.out.print("\033[" + linesUp + "A\r\033[2K");

        int filled = (progress * BAR_WIDTH) / 100;
        String bar = "#".repeat(filled) + ".".repeat(BAR_WIDTH - filled);

        if (elapsedMs >= 0) {
            System.out.printf("Thread %2d (id=%3d) [%s] done in %.1fs", taskId, threadId, bar, elapsedMs / 1000.0);
        } else {
            System.out.printf("Thread %2d (id=%3d) [%s] %3d%%", taskId, threadId, bar, progress);
        }

        System.out.print("\033[" + linesUp + "B\r");
        System.out.flush();
    }
}