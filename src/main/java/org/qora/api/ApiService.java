package org.qora.api;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

import org.eclipse.jetty.rewrite.handler.RedirectPatternRule;
import org.eclipse.jetty.rewrite.handler.RewriteHandler;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.handler.InetAccessHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.qora.api.resource.AnnotationPostProcessor;
import org.qora.api.resource.ApiDefinition;
import org.qora.settings.Settings;

public class ApiService {

	private final Server server;
	private final ResourceConfig config;

	public ApiService() {
		config = new ResourceConfig();
		config.packages("org.qora.api.resource");
		config.register(OpenApiResource.class);
		config.register(ApiDefinition.class);
		config.register(AnnotationPostProcessor.class);

		// Create RPC server
		this.server = new Server(Settings.getInstance().getApiPort());

		// Error handler
		ErrorHandler errorHandler = new ApiErrorHandler();
		this.server.setErrorHandler(errorHandler);

		// Request logging
		if (Settings.getInstance().isApiLoggingEnabled()) {
			RequestLogWriter logWriter = new RequestLogWriter("API-requests.log");
			logWriter.setAppend(true);
			logWriter.setTimeZone("UTC");
			RequestLog requestLog = new CustomRequestLog(logWriter, CustomRequestLog.EXTENDED_NCSA_FORMAT);
			server.setRequestLog(requestLog);
		}

		// IP address based access control
		InetAccessHandler accessHandler = new InetAccessHandler();
		for (String pattern : Settings.getInstance().getApiWhitelist()) {
			accessHandler.include(pattern);
		}
		this.server.setHandler(accessHandler);

		// URL rewriting
		RewriteHandler rewriteHandler = new RewriteHandler();
		accessHandler.setHandler(rewriteHandler);

		// Context
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		context.setContextPath("/");
		rewriteHandler.setHandler(context);

		// Cross-origin resource sharing
		FilterHolder corsFilterHolder = new FilterHolder(CrossOriginFilter.class);
		corsFilterHolder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
		corsFilterHolder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET, POST, DELETE");
		corsFilterHolder.setInitParameter(CrossOriginFilter.CHAIN_PREFLIGHT_PARAM, "false");
		context.addFilter(corsFilterHolder, "/*", null);

		// API servlet
		ServletContainer container = new ServletContainer(config);
		ServletHolder apiServlet = new ServletHolder(container);
		apiServlet.setInitOrder(1);
		context.addServlet(apiServlet, "/*");

		// Swagger-UI static content
		ClassLoader loader = this.getClass().getClassLoader();
		ServletHolder swaggerUIServlet = new ServletHolder("static-swagger-ui", DefaultServlet.class);
		swaggerUIServlet.setInitParameter("resourceBase", loader.getResource("resources/swagger-ui/").toString());
		swaggerUIServlet.setInitParameter("dirAllowed", "true");
		swaggerUIServlet.setInitParameter("pathInfoOnly", "true");
		context.addServlet(swaggerUIServlet, "/api-documentation/*");

		rewriteHandler.addRule(new RedirectPatternRule("", "/api-documentation/")); // redirect to Swagger UI start page
		rewriteHandler.addRule(new RedirectPatternRule("/api-documentation", "/api-documentation/")); // redirect to Swagger UI start page
	}

	// XXX: replace singleton pattern by dependency injection?
	private static ApiService instance;

	public static ApiService getInstance() {
		if (instance == null) {
			instance = new ApiService();
		}

		return instance;
	}

	public Iterable<Class<?>> getResources() {
		// return resources;
		return config.getClasses();
	}

	public void start() {
		try {
			// Start server
			server.start();
		} catch (Exception e) {
			// Failed to start
			throw new RuntimeException("Failed to start API", e);
		}
	}

	public void stop() {
		try {
			// Stop server
			server.stop();
		} catch (Exception e) {
			// Failed to stop
		}
	}
}
