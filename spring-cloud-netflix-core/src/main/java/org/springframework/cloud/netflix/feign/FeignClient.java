package org.springframework.cloud.netflix.feign;

import java.lang.annotation.*;

/**
 * @author Spencer Gibb
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClient {
	/**
	 * @return serviceId if loadbalance is true, url otherwise
	 * No need to prefix serviceId with http://
	 */
	String value();
	boolean loadbalance() default true;
}
