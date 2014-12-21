package org.springframework.cloud.netflix.ribbon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequest;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient.RibbonServer;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerStats;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerStats;

/**
 * @author Spencer Gibb
 */
public class RibbonLoadBalancerClientTests {

    @Mock
    SpringClientFactory clientFactory;

    @Mock
    BaseLoadBalancer loadBalancer;

    @Mock
    LoadBalancerStats loadBalancerStats;

    @Mock
    ServerStats serverStats;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(clientFactory.getLoadBalancerContext(anyString())).thenReturn(new RibbonLoadBalancerContext(loadBalancer));
    }

    @Test
    public void reconstructURI() throws Exception {
        RibbonServer server = getRibbonServer();
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        ServiceInstance serviceInstance = client.choose(server.getServiceId());
        URI uri = client.reconstructURI(serviceInstance, new URL("http://" + server.serviceId).toURI());
        assertEquals(server.getHost(), uri.getHost());
        assertEquals(server.getPort(), uri.getPort());
    }

    @Test
    public void testChoose() {
        RibbonServer server = getRibbonServer();
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);
        ServiceInstance serviceInstance = client.choose(server.getServiceId());
        assertServiceInstance(server, serviceInstance);
    }

    @Test
    public void testExecute() {
        final RibbonServer server = getRibbonServer();
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(server);

        final String returnVal = "myval";
        Object actualReturn = client.execute(server.getServiceId(), new LoadBalancerRequest<Object>() {
            @Override
            public Object apply(ServiceInstance instance) throws Exception {
                assertServiceInstance(server, instance);
                return returnVal;
            }
        });

        verifyServerStats();

        assertEquals("retVal was wrong", returnVal, actualReturn);
    }


    @Test
    public void testExecuteException() {
        final RibbonServer ribbonServer = getRibbonServer();
        RibbonLoadBalancerClient client = getRibbonLoadBalancerClient(ribbonServer);

        try {
            client.execute(ribbonServer.getServiceId(), new LoadBalancerRequest<Object>() {
                @Override
                public Object apply(ServiceInstance instance) throws Exception {
                    assertServiceInstance(ribbonServer, instance);
                    throw new RuntimeException();
                }
            });
            fail("Should have thrown exception");
        } catch (Exception e) {
            assertNotNull(e);
        }

        verifyServerStats();
    }

    protected RibbonServer getRibbonServer() {
        return new RibbonServer("testService", new Server("myhost", 9080));
    }

    protected void verifyServerStats() {
        verify(serverStats).incrementActiveRequestsCount();
        verify(serverStats).decrementActiveRequestsCount();
        verify(serverStats).incrementNumRequests();
        verify(serverStats).noteResponseTime(anyDouble());
    }

    protected void assertServiceInstance(RibbonServer ribbonServer, ServiceInstance instance) {
        assertNotNull("instance was null", instance);
        assertEquals("serviceId was wrong", ribbonServer.getServiceId(), instance.getServiceId());
        assertEquals("host was wrong", ribbonServer.getHost(), instance.getHost());
        assertEquals("port was wrong", ribbonServer.getPort(), instance.getPort());
    }

    protected RibbonLoadBalancerClient getRibbonLoadBalancerClient(RibbonServer ribbonServer) {
		when(loadBalancer.getName()).thenReturn(ribbonServer.getServiceId());
        when(loadBalancer.chooseServer(anyString())).thenReturn(ribbonServer.server);
        when(loadBalancer.getLoadBalancerStats()).thenReturn(loadBalancerStats);
        when(loadBalancerStats.getSingleServerStat(ribbonServer.server)).thenReturn(serverStats);
        when(clientFactory.getLoadBalancer(loadBalancer.getName())).thenReturn(loadBalancer);

        return new RibbonLoadBalancerClient(clientFactory);
    }
}
