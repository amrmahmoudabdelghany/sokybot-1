package org.sokybot.webview;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class WebviewActivator {

    private static final Logger logger = LoggerFactory.getLogger(WebviewActivator.class);

    @Activate
    public void start(BundleContext context) {
        logger.info("Webview Bundle activated");
    }

    @Deactivate
    public void stop() {
        logger.info("Webview Bundle deactivated");
    }
}
