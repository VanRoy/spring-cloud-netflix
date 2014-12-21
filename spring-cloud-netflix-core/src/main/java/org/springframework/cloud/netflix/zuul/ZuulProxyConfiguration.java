package org.springframework.cloud.netflix.zuul;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.DiscoveryHeartbeatEvent;
import org.springframework.cloud.client.discovery.InstanceRegisteredEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.pre.PreDecorationFilter;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.SimpleHostRoutingFilter;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Spencer Gibb
 * @author Dave Syer
 */
@Configuration
public class ZuulProxyConfiguration extends ZuulConfiguration {

	@Autowired(required = false)
	private TraceRepository traces;

	@Autowired
	private SpringClientFactory clientFactory;

	@Autowired
	private DiscoveryClient discovery;

	@Autowired
	private ZuulProperties zuulProperties;

	@Bean
	@Override
	public ProxyRouteLocator routes() {
		return new ProxyRouteLocator(discovery, zuulProperties);
	}

	@Bean
	// @RefreshScope
	public RoutesEndpoint zuulEndpoint() {
		return new RoutesEndpoint(routes());
	}

	// pre filters
	@Bean
	public PreDecorationFilter preDecorationFilter() {
		return new PreDecorationFilter(routes(), zuulProperties);
	}

	// route filters
	@Bean
	public RibbonRoutingFilter ribbonRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (traces != null) {
			helper.setTraces(traces);
		}
		RibbonRoutingFilter filter = new RibbonRoutingFilter(helper, clientFactory);
		return filter;
	}

	@Bean
	public SimpleHostRoutingFilter simpleHostRoutingFilter() {
		ProxyRequestHelper helper = new ProxyRequestHelper();
		if (traces != null) {
			helper.setTraces(traces);
		}
		return new SimpleHostRoutingFilter(helper);
	}

	@Bean
	@Override
	public ApplicationListener<ApplicationEvent> zuulRefreshRoutesListener() {
		return new ZuulRefreshListener();
	}

	private static class ZuulRefreshListener implements
			ApplicationListener<ApplicationEvent> {

		private AtomicReference<Object> latestHeartbeat = new AtomicReference<>();

		@Autowired
		private ProxyRouteLocator routeLocator;

		@Autowired
		ZuulHandlerMapping zuulHandlerMapping;

		@Override
		public void onApplicationEvent(ApplicationEvent event) {
			if (event instanceof InstanceRegisteredEvent
					|| event instanceof RefreshScopeRefreshedEvent
					|| event instanceof RoutesRefreshedEvent) {
				reset();
			}
			else if (event instanceof DiscoveryHeartbeatEvent) {
				DiscoveryHeartbeatEvent e = (DiscoveryHeartbeatEvent) event;
				if (latestHeartbeat.get() == null
						|| !latestHeartbeat.get().equals(e.getValue())) {
					latestHeartbeat.set(e.getValue());
					reset();
				}
			}

		}

		private void reset() {
			routeLocator.resetRoutes();
			zuulHandlerMapping.registerHandlers();
		}

	}

}
