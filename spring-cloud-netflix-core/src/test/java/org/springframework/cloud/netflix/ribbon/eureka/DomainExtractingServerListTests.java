package org.springframework.cloud.netflix.ribbon.eureka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;

/**
 * @author Spencer Gibb
 */
public class DomainExtractingServerListTests {

    static final String IP_ADDR = "10.0.0.2";
    static final int PORT = 8080;
    static final String ZONE = "myzone.mydomain.com";
    static final String HOST_NAME = "myHostName."+ZONE;
    static final String INSTANCE_ID = "myInstanceId";

    @Test
    public void testDomainExtractingServer() {
        DomainExtractingServerList serverList = getDomainExtractingServerList(new DefaultClientConfigImpl(), true);

        List<Server> servers = serverList.getInitialListOfServers();
        assertNotNull("servers was null", servers);
        assertEquals("servers was not size 1", 1, servers.size());

        DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
        assertEquals("hostPort was wrong", HOST_NAME+":"+PORT, des.getHostPort());
    }

	@Test
	public void testDomainExtractingServerDontApproximateZone() {
		DomainExtractingServerList serverList = getDomainExtractingServerList(new DefaultClientConfigImpl(), false);

		List<Server> servers = serverList.getInitialListOfServers();
		assertNotNull("servers was null", servers);
		assertEquals("servers was not size 1", 1, servers.size());

		DomainExtractingServer des = assertDomainExtractingServer(servers, null);
		assertEquals("hostPort was wrong", HOST_NAME+":"+PORT, des.getHostPort());
	}

    protected DomainExtractingServer assertDomainExtractingServer(List<Server> servers, String zone) {
        Server actualServer = servers.get(0);
        assertTrue("server was not a DomainExtractingServer", actualServer instanceof DomainExtractingServer);
        DomainExtractingServer des = DomainExtractingServer.class.cast(actualServer);
        assertEquals("zone was wrong", zone, des.getZone());
        assertEquals("instanceId was wrong", INSTANCE_ID, des.getId());
        return des;
    }

    @Test
    public void testDomainExtractingServerUseIpAddress() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.setProperty(CommonClientConfigKey.UseIPAddrForServer, true);
        DomainExtractingServerList serverList = getDomainExtractingServerList(config, true);

        List<Server> servers = serverList.getInitialListOfServers();
        assertNotNull("servers was null", servers);
        assertEquals("servers was not size 1", 1, servers.size());

        DomainExtractingServer des = assertDomainExtractingServer(servers, ZONE);
        assertEquals("hostPort was wrong", IP_ADDR+":"+PORT, des.getHostPort());
    }

    protected DomainExtractingServerList getDomainExtractingServerList(DefaultClientConfigImpl config, boolean approximateZoneFromHostname) {
        DiscoveryEnabledServer server = mock(DiscoveryEnabledServer.class);
		@SuppressWarnings("unchecked")
		ServerList<Server> originalServerList = mock(ServerList.class);
        InstanceInfo instanceInfo = mock(InstanceInfo.class);

        when(server.getInstanceInfo()).thenReturn(instanceInfo);
        when(server.getHost()).thenReturn(HOST_NAME);

        when(instanceInfo.getMetadata()).thenReturn(ImmutableMap.<String, String>builder().put("instanceId", INSTANCE_ID).build());
        when(instanceInfo.getHostName()).thenReturn(HOST_NAME);
        when(instanceInfo.getIPAddr()).thenReturn(IP_ADDR);
        when(instanceInfo.getPort()).thenReturn(PORT);

        when(originalServerList.getInitialListOfServers()).thenReturn(Arrays.<Server>asList(server));

        return new DomainExtractingServerList(originalServerList, config, approximateZoneFromHostname);
    }

}
