package org.springframework.cloud.netflix.zuul;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */
@Data
@ConfigurationProperties("zuul")
public class ZuulProperties {
    private String mapping = "";
    private boolean stripMapping = false;
    private Map<String,String> route = new HashMap<String, String>();
    private boolean addProxyHeaders = true;
    private List<String> ignoredServices = Collections.emptyList();
}
