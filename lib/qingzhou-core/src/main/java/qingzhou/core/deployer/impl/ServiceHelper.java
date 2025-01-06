package qingzhou.core.deployer.impl;

import java.util.HashSet;
import java.util.Set;

import qingzhou.engine.ModuleContext;
import qingzhou.engine.ServiceListener;
import qingzhou.engine.util.pattern.Process;

public class ServiceHelper implements Process {
    static Set<Class<?>> injectedServicesBackupForCore = new HashSet<>();

    private static final Set<Class<?>> ALL_GOT_SERVICES = new HashSet<>();
    private final ModuleContext context;


    ServiceHelper(ModuleContext context) {
        this.context = context;
    }

    public static boolean isServiceInUse(Class<?> serviceType) {
        if (injectedServicesBackupForCore.contains(serviceType)) return true;

        return ALL_GOT_SERVICES.contains(serviceType);
    }

    @Override
    public void exec() throws Throwable {
        context.addServiceListener((event, serviceType) -> {
            if (ServiceListener.ServiceEvent.GOT == event) {
                ALL_GOT_SERVICES.add(serviceType);
            }
        });
    }
}
