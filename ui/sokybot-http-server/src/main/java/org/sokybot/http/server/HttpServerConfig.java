package org.sokybot.http.server;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Sokybot HTTP Server Configuration", description = "Configuration for the shared Vert.x HTTP server")
public @interface HttpServerConfig {

    @AttributeDefinition(name = "HTTP Port", description = "Port to bind the HTTP server to")
    int port() default 8182;

    @AttributeDefinition(name = "HTTP Host", description = "Host/Interface to bind the HTTP server to")
    String host() default "0.0.0.0";

    @AttributeDefinition(name = "Dev Mode", description = "Enable development proxy features")
    boolean devMode() default true;

    @AttributeDefinition(name = "Webview Dev URL", description = "URL of the Webview Vite server")
    String webviewDevUrl() default "http://localhost:5173";

    @AttributeDefinition(name = "DevTools Dev URL", description = "URL of the DevTools Vite server")
    String devtoolsDevUrl() default "http://localhost:3000";

    @AttributeDefinition(name = "Static Root", description = "Classpath root for bundled production web assets")
    String staticRoot() default "webapp";
}
