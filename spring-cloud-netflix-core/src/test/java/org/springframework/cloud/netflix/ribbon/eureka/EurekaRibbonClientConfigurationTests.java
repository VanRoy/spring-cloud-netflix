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
package org.springframework.cloud.netflix.ribbon.eureka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.netflix.ribbon.eureka.EurekaRibbonClientConfiguration.VALUE_NOT_SET;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;

import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext.ContextKey;
import com.netflix.config.DynamicStringProperty;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAwareLoadBalancer;

/**
 * @author Dave Syer
 *
 */
public class EurekaRibbonClientConfigurationTests {

	@After
	public void close() {
		ConfigurationManager.getDeploymentContext().setValue(ContextKey.zone, "");
	}

	@Test
	@Ignore
	public void basicConfigurationCreatedForLoadBalancer() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		client.getAvailabilityZones().put(client.getRegion(), "foo");
		SpringClientFactory clientFactory = new SpringClientFactory();
		EurekaRibbonClientConfiguration clientPreprocessor = new EurekaRibbonClientConfiguration(
				client, "service");
		clientPreprocessor.preprocess();
		ILoadBalancer balancer = clientFactory.getLoadBalancer("service");
		assertNotNull(balancer);
		@SuppressWarnings("unchecked")
		ZoneAwareLoadBalancer<Server> aware = (ZoneAwareLoadBalancer<Server>) balancer;
		assertTrue(aware.getServerListImpl() instanceof DomainExtractingServerList);
		assertEquals("foo",
				ConfigurationManager.getDeploymentContext().getValue(ContextKey.zone));
	}

	@Test
	public void testSetProp() {
		EurekaClientConfigBean client = new EurekaClientConfigBean();
		EurekaRibbonClientConfiguration preprocessor = new EurekaRibbonClientConfiguration(
				client, "myService");

		String serviceId = "myService";
		String suffix = "mySuffix";
		String value = "myValue";

		DynamicStringProperty property = preprocessor.getProperty(preprocessor.getKey(
				serviceId, suffix));

		assertEquals("property doesn't have default value", VALUE_NOT_SET, property.get());

		preprocessor.setProp(serviceId, suffix, value);

		assertEquals("property has wrong value", value, property.get());

		preprocessor.setProp(serviceId, suffix, value);

		assertEquals("property has wrong value", value, property.get());
	}

}
