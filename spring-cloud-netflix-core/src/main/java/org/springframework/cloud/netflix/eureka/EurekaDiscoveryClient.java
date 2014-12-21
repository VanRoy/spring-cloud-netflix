package org.springframework.cloud.netflix.eureka;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Iterables.*;

/**
 * @author Spencer Gibb
 */
public class EurekaDiscoveryClient implements DiscoveryClient {

	public static final String DESCRIPTION = "Spring Cloud Eureka Discovery Client";

	@Autowired
    private EurekaInstanceConfigBean config;

    @Autowired
    private com.netflix.discovery.DiscoveryClient discovery;

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
    public ServiceInstance getLocalServiceInstance() {
        return new ServiceInstance() {
            @Override
            public String getServiceId() {
                return config.getAppname();
            }

            @Override
            public String getHost() {
                return config.getHostname();
            }

            @Override
            public int getPort() {
                return config.getNonSecurePort();
            }
        };
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        List<InstanceInfo> infos = discovery.getInstancesByVipAddress(serviceId, false);
        Iterable<ServiceInstance> instances = transform(infos, new Function<InstanceInfo, ServiceInstance>() {
            @Nullable
            @Override
            public ServiceInstance apply(@Nullable InstanceInfo info) {
                return new EurekaServiceInstance(info);
            }
        });
        return Lists.newArrayList(instances);
    }

    static class EurekaServiceInstance implements ServiceInstance {
        InstanceInfo instance;

        EurekaServiceInstance(InstanceInfo instance) {
            this.instance = instance;
        }

        @Override
        public String getServiceId() {
            return instance.getAppName();
        }

        @Override
        public String getHost() {
            return instance.getHostName();
        }

        @Override
        public int getPort() {
            return instance.getPort();
        }
    }

    @Override
    public List<String> getServices() {
        Applications applications = discovery.getApplications();
        if (applications == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(filter(transform(applications.getRegisteredApplications(), new Function<Application, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Application app) {
                if (app.getInstances().isEmpty()) {
                    return null;
                }
                return app.getName().toLowerCase();
            }
        }), Predicates.notNull()));
    }

    @Override
    public List<ServiceInstance> getAllInstances() {
        Applications applications = discovery.getApplications();
        if (applications == null) {
            return Collections.emptyList();
        }
        Iterable<ServiceInstance> instances = transform(concat(transform(applications.getRegisteredApplications(), new Function<Application, List<InstanceInfo>>() {
            public List<InstanceInfo> apply(@Nullable Application app) {
                return app.getInstances();
            }
        })), new Function<InstanceInfo, ServiceInstance>() {
            @Nullable
            @Override
            public ServiceInstance apply(@Nullable InstanceInfo info) {
                return new EurekaServiceInstance(info);
            }
        });
        return Lists.newArrayList(instances);
    }
}
