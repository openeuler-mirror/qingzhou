package qingzhou.app.system.jmx;

import com.sun.jmx.remote.security.MBeanServerAccessController;

import javax.management.ObjectName;

class CustomMBeanServerAccessController extends MBeanServerAccessController {

    @Override
    protected void checkRead() {
    }

    @Override
    protected void checkWrite() {
    }

    @Override
    protected void checkCreate(String className) {
        throw new SecurityException("Access denied!");
    }

    @Override
    public void unregisterMBean(ObjectName name) {
        throw new SecurityException("Access denied!");
    }
}
