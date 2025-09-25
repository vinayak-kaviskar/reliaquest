package com.reliaquest.api.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.dto.EmployeeRequest;
import com.reliaquest.api.model.Employee;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EmployeesIntegrationTest {

    private static List<Employee> cachedEmployees;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;

    @BeforeEach
    void setUp() {

        baseUrl = "http://localhost:" + port + "/api/v1/employee";
    }

    @Test
    @Order(1)
    void getAllEmployees_ShouldReturnAllEmployees() {

        ResponseEntity<List<Employee>> response =
                restTemplate.exchange(baseUrl, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), not(empty()));
        assertThat(response.getBody().get(0).getId(), notNullValue());

        cachedEmployees = response.getBody();
    }

    @Test
    @Order(2)
    void getEmployeeById_WhenEmployeeExists_ShouldReturnEmployee() {

        String existingEmployeeId = cachedEmployees.get(0).getId();
        ResponseEntity<Employee> response =
                restTemplate.getForEntity(baseUrl + "/" + existingEmployeeId, Employee.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), notNullValue());
        assertEquals(existingEmployeeId, response.getBody().getId());
    }

    @Test
    @Order(3)
    void getEmployeeById_WhenEmployeeNotExists_ShouldReturn404() {

        String nonExistentId = UUID.randomUUID().toString();
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + nonExistentId, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @Order(4)
    void getEmployeeById_WhenInValidEmployeeIdFormat_ShouldReturn404() {

        String nonExistentId = "invalid id string";
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/" + nonExistentId, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(5)
    void createEmployee_WithValidData_ShouldCreateEmployee() {

        EmployeeRequest request = new EmployeeRequest("Test Employee", 50000, 30, "Tester");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Employee> response = restTemplate.postForEntity(baseUrl, entity, Employee.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), notNullValue());
        assertEquals("Test Employee", response.getBody().getEmployeeName());
        assertEquals(50000, response.getBody().getEmployeeSalary());
    }

    @Test
    @Order(6)
    void createEmployee_WithInvalidData_ShouldReturn400() {

        EmployeeRequest invalidRequest = new EmployeeRequest("", 0, 0, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmployeeRequest> entity = new HttpEntity<>(invalidRequest, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @Order(7)
    void deleteEmployee_WhenEmployeeExists_ShouldReturnEmployeeName() {

        EmployeeRequest request = new EmployeeRequest("To Delete", 30000, 25, "Temp");
        ResponseEntity<Employee> createResponse = restTemplate.postForEntity(baseUrl, request, Employee.class);
        String employeeId = createResponse.getBody().getId();
        ResponseEntity<String> response =
                restTemplate.exchange(baseUrl + "/" + employeeId, HttpMethod.DELETE, null, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("To Delete", response.getBody());
    }

    @Test
    @Order(8)
    void getHighestSalary_ShouldReturnHighestSalary() {

        ResponseEntity<Integer> response = restTemplate.getForEntity(baseUrl + "/highestSalary", Integer.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), greaterThan(0));
    }

    @Test
    @Order(9)
    void getTopTenHighestEarningEmployeeNames_ShouldReturnTop10Employees() {

        ResponseEntity<List<String>> response = restTemplate.exchange(
                baseUrl + "/topTenHighestEarningEmployeeNames",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), hasSize(lessThanOrEqualTo(10)));
        assertThat(response.getBody().get(0), not(emptyString()));
    }

    @Test
    @Order(10)
    void searchEmployees_WithValidName_ShouldReturnMatchingEmployees() {

        String searchTerm = cachedEmployees.get(0).getEmployeeName();
        ResponseEntity<List<Employee>> response = restTemplate.exchange(
                baseUrl + "/search/" + searchTerm, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getBody(), not(empty()));
        assertThat(response.getBody().get(0).getEmployeeName().toLowerCase(), containsString(searchTerm.toLowerCase()));
    }
}
