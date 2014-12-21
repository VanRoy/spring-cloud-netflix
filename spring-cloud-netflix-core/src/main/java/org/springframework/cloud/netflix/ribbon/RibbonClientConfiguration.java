/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.netflix.ribbon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.ribbon.eureka.DomainExtractingServerList;
import org.springframework.cloud.netflix.ribbon.eureka.ZonePreferenceServerListFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.DynamicServerListLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.loadbalancer.ServerListFilter;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;
import com.netflix.niws.client.http.RestClient;
import com.netflix.servo.monitor.Monitors;

/**
 * @author Dave Syer
 *
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableConfigurationProperties
public class RibbonClientConfiguration {

	@Value("${ribbon.client.name}")
	private String name = "client";

	@Value("${ribbon.eureka.approximateZoneFromHostname:false}")
	private boolean approximateZoneFromHostname = false;

	// TODO: maybe re-instate autowired load balancers: identified by name they could be
	// associated with ribbon clients
	
	@Bean
	@ConditionalOnMissingBean
	public IClientConfig ribbonClientConfig() {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.loadProperties(name);
		return config;
	}

	@Bean
	@ConditionalOnMissingBean
	public RestClient ribbonRestClient(IClientConfig config, ILoadBalancer loadBalancer) {
		RestClient client = new RestClient(config);
		client.setLoadBalancer(loadBalancer);
		Monitors.registerObject("Client_" + name, client);
		return client;
	}

	@Bean
	@ConditionalOnMissingBean
	//TODO: move to ribbon.eureka package
	public ILoadBalancer ribbonLoadBalancer(IClientConfig config, ServerListFilter<Server> filter) {
		ZoneAwareLoadBalancer<Server> balancer = new ZoneAwareLoadBalancer<>(config);
		wrapServerList(balancer);
		balancer.setFilter(filter);
		return balancer;
	}
	
	@Bean
	@ConditionalOnMissingBean
	public ServerListFilter<Server> ribbonServerListFilter(IClientConfig config) {
		ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
		filter.initWithNiwsConfig(config);
		return filter;
	}
	
	@Bean
	@ConditionalOnMissingBean
	public RibbonLoadBalancerContext ribbonLoadBalancerContext(ILoadBalancer loadBalancer, IClientConfig config) {
		return new RibbonLoadBalancerContext(loadBalancer, config);
	}

	private void wrapServerList(ILoadBalancer balancer) {
		if (balancer instanceof DynamicServerListLoadBalancer) {
			@SuppressWarnings("unchecked")
			DynamicServerListLoadBalancer<Server> dynamic = (DynamicServerListLoadBalancer<Server>) balancer;
			ServerList<Server> list = dynamic.getServerListImpl();
			if (!(list instanceof DomainExtractingServerList)) {
				// This is optional: you can use the native Eureka AWS features as long as
				// the server zone is populated. TODO: verify that we back off if AWS
				// metadata *is* available.
				// @see com.netflix.appinfo.AmazonInfo.Builder
				dynamic.setServerListImpl(new DomainExtractingServerList(list, dynamic
						.getClientConfig(), approximateZoneFromHostname));
			}
		}
	}

}
