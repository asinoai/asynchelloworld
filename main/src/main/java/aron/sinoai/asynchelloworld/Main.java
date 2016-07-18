package aron.sinoai.asynchelloworld;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main {
    final ExecutorService pool = Executors.newFixedThreadPool(4);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("Start!");

        final Main main = new Main();
        main.simpleAsyncHello();
        main.betterAsyncHello();
        main.oldWayAsyncHello();
        main.oldWayQueuedAsyncHello();

        main.gracefullyShutdownPool();
    }

    public void simpleAsyncHello() throws InterruptedException, ExecutionException {

        //this in not blocking!
        pool.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("Hello from thread!");
            }
        });

        //this is not blocking!
        final Future<String> futureValue = pool.submit(new Callable<String>() {

            @Override
            public String call() throws Exception {
                return "another thread";
            }
        });

        //this is blocking call but the value is calculated on a separate thread!
        final String value = futureValue.get();
        System.out.println("Hello from main thread with value get from " + value);

    }

    public void betterAsyncHello() throws InterruptedException, ExecutionException {

        //this is not blocking!
        final CompletableFuture<String> valueCalculationPosting = CompletableFuture.supplyAsync(new Supplier<String>() {
            @Override
            public String get() {
                try {
                    Thread.sleep(100);
                } catch (final InterruptedException e) {
                    System.err.println("Should not happen!");
                }

                return "3rd thread";
            }
        }, pool);

        //this is not blocking!
        valueCalculationPosting.thenAccept(new Consumer<String>() {
            @Override
            public void accept(String value) {
                System.out.println("Hello from " + value);
            }
        });

        System.out.println("Hello from main thread!");
    }

    private void oldWayAsyncHello() {
        final Thread thread = new Thread() {
            @Override
            public void run() {
                System.out.println("Hello form non pool thread!");
            }
        };

        thread.start();
    }

    public void gracefullyShutdownPool() {

        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private void oldWayQueuedAsyncHello() throws InterruptedException {
        final CustomThread thread = new CustomThread();

        thread.start();

        System.out.println("Numeric value from thread (before take): " +
                thread.getNumericValue() + ", " +
                thread.addAtomicNumericValue(5));

        final String value = thread.getQueue().take();
        System.out.println(value);

        System.out.println("Numeric value from thread (after take): " +
                thread.getNumericValue() + ", " +
                thread.getAtomicNumericValue());
    }

    private static class CustomThread extends Thread {
        private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();
        private final AtomicInteger atomicNumericValue = new AtomicInteger(0);
        private volatile int numericValue = 0;

        @Override
        public void run() {
            try {
                numericValue = numericValue + 10;

                atomicNumericValue.addAndGet(5);

                queue.put("Hello form thread with blocking queue!");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public BlockingDeque<String> getQueue() {
            return queue;
        }

        public int getNumericValue() {
            return numericValue;
        }

        public int addAtomicNumericValue(final int value) {
            return atomicNumericValue.addAndGet(value);
        }

        public int getAtomicNumericValue() {
            return atomicNumericValue.get();
        }
    }
}
