package qingzhou.logger.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import qingzhou.logger.Logger;

@Component(immediate = true)
public class Slf4jBridgeActivator {
    @Reference
    private Logger logger;

    @Activate
    public void activate() {
        Slf4jBridge.setQingzhouLogger(logger);
    }

    @Deactivate
    public void deactivate() {
        Slf4jBridge.setQingzhouLogger(null);
    }
}
