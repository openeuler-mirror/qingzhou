package qingzhou.app.redis.collector;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CollectorScheduler {

    private static volatile CollectorScheduler instance;
    private ScheduledExecutorService scheduler;
    private final InstanceCollector instanceCollector;
    private final MachineCollector machineCollector;
    private final Runnable alertCheckTask;
    private final Runnable diagnoseTask;
    private final Runnable slowLogTask;

    private CollectorScheduler(InstanceCollector instanceCollector,
                               MachineCollector machineCollector,
                               Runnable alertCheckTask,
                               Runnable diagnoseTask,
                               Runnable slowLogTask) {
        this.instanceCollector = instanceCollector;
        this.machineCollector = machineCollector;
        this.alertCheckTask = alertCheckTask;
        this.diagnoseTask = diagnoseTask;
        this.slowLogTask = slowLogTask;
    }

    public static synchronized void initialize(InstanceCollector instanceCollector,
                                                MachineCollector machineCollector,
                                                Runnable alertCheckTask,
                                                Runnable diagnoseTask,
                                                Runnable slowLogTask) {
        if (instance != null) {
            instance.shutdown();
        }
        instance = new CollectorScheduler(instanceCollector, machineCollector, alertCheckTask, diagnoseTask, slowLogTask);
        instance.start();
    }

    public static CollectorScheduler getInstance() {
        return instance;
    }

    public static synchronized void shutdownInstance() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }

    private void start() {
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "redis-collector");
            t.setDaemon(true);
            return t;
        });


        scheduler.scheduleAtFixedRate(wrap(instanceCollector::collect), 5, 30, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(wrap(machineCollector::collect), 5, 30, TimeUnit.SECONDS);

        if (slowLogTask != null) {
            scheduler.scheduleAtFixedRate(wrap(slowLogTask), 10, 60, TimeUnit.SECONDS);
        }

        if (alertCheckTask != null) {
            scheduler.scheduleAtFixedRate(wrap(alertCheckTask), 15, 60, TimeUnit.SECONDS);
        }

        if (diagnoseTask != null) {
            scheduler.scheduleAtFixedRate(wrap(diagnoseTask), 20, 5, TimeUnit.MINUTES);
        }
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
            }
        };
    }

    public void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }
}
