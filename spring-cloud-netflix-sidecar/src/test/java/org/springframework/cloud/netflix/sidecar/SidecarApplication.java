package org.springframework.cloud.netflix.sidecar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableSidecar
@RestController
public class SidecarApplication {
	public static void main(String[] args) {
        SpringApplication.run(SidecarApplication.class, args);
	}
}
