package org.niit_project.backend;

import org.junit.jupiter.api.Test;
import org.niit_project.backend.model.User;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BackendApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testSignup() {
		User newUser = new User();
		newUser.setUsername("newuser");
		newUser.setPassword("newpass");
		newUser.setEmail("newuser@example.com");

		ResponseEntity<User> response = restTemplate.postForEntity("/auth/signup", newUser, User.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getUsername()).isEqualTo("newuser");
	}

	@Test
	public void testLogin() {
		User loginRequest = new User();
		loginRequest.setUsername("testuser");
		loginRequest.setPassword("password123");

		ResponseEntity<User> response = restTemplate.postForEntity("/auth/login", loginRequest, User.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
