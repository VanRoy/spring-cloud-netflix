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

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Dave Syer
 *
 */
public class RibbonClientConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
			BeanDefinitionRegistry registry) {
		Map<String, Object> attrs = metadata.getAnnotationAttributes(
				RibbonClients.class.getName(), true);
		if (attrs != null && attrs.containsKey("value")) {
			AnnotationAttributes[] clients = (AnnotationAttributes[]) attrs.get("value");
			for (AnnotationAttributes client : clients) {
				registerClientConfiguration(registry, client.get("name"),
						client.get("configuration"));
			}
		}
		if (attrs != null && attrs.containsKey("defaultConfiguration")) {
			registerClientConfiguration(registry, "default." + metadata.getEnclosingClassName(),
					attrs.get("defaultConfiguration"));
		}
		Map<String, Object> client = metadata.getAnnotationAttributes(
				RibbonClient.class.getName(), true);
		if (client != null && client.containsKey("name")) {
			registerClientConfiguration(registry, client.get("name"),
					client.get("configuration"));
		}
	}

	private void registerClientConfiguration(BeanDefinitionRegistry registry,
			Object name, Object configuration) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(RibbonClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + ".RibbonClientSpecification",
				builder.getBeanDefinition());
	}

}
