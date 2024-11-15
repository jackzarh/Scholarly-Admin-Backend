package org.niit_project.backend;

import org.junit.jupiter.api.Test;
import org.niit_project.backend.entities.Admin;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BackendApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testSignup() {
		Admin newAdmin = new Admin();
		newAdmin.setFirstName("newuser");
		newAdmin.setPassword("newpass");
		newAdmin.setEmail("newuser@example.com");

		ResponseEntity<Admin> response = restTemplate.postForEntity("/auth/signup", newAdmin, Admin.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getFirstName()).isEqualTo("newuser");
	}

	@Test
	public void testLogin() {
		Admin loginRequest = new Admin();
		loginRequest.setFirstName("testuser");
		loginRequest.setPassword("password123");

		ResponseEntity<Admin> response = restTemplate.postForEntity("/auth/login", loginRequest, Admin.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
