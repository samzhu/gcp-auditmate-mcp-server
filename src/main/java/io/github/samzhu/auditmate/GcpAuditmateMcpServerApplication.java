package io.github.samzhu.auditmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GcpAuditmateMcpServerApplication {

	public static void main(String[] args) {
		System.setProperty("file.encoding", "UTF-8");
		SpringApplication.run(GcpAuditmateMcpServerApplication.class, args);
	}

}
