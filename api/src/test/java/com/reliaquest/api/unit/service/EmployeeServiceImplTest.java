package com.reliaquest.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeRequest;
import com.reliaquest.api.exception.EntityNotFoundException;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Service Implementation Tests")
class EmployeeServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private String employeeId;
    private EmployeeRequest employeeRequest;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(restTemplate);
        employeeId = UUID.randomUUID().toString();
        testEmployee = new Employee(employeeId, "John Doe", 50000, 25, "Developer", "IT");
        employeeRequest = new EmployeeRequest("John Doe", 50000, 25, "Developer");

        ReflectionTestUtils.setField(employeeService, "domain", "http://test-domain.com");
        ReflectionTestUtils.setField(employeeService, "basePath", "/api/v1/employee");
    }

    @Test
    @DisplayName("Should get all employees successfully")
    void getAllEmployees_Success() {

        List<Employee> expectedEmployees = Collections.singletonList(testEmployee);
        ApiResponse<List<Employee>> apiResponse = new ApiResponse<>();
        apiResponse.setData(expectedEmployees);
        apiResponse.setStatus("success");
        ResponseEntity<ApiResponse<List<Employee>>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        String expectedUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        List<Employee> result = employeeService.getAllEmployees();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testEmployee.getEmployeeName(), result.get(0).getEmployeeName());
    }

    @Test
    @DisplayName("Should throw ExternalServiceException when external service returns null")
    void getAllEmployees_ExternalServiceReturnsNull_ThrowsException() {

        String expectedUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThrows(ExternalServiceException.class, () -> employeeService.getAllEmployees());
    }

    @Test
    @DisplayName("Should throw TooManyRequestsException when rate limit exceeded")
    void getAllEmployees_RateLimitExceeded_ThrowsTooManyRequestsException() {

        String expectedUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(TooManyRequestsException.class, () -> employeeService.getAllEmployees());
    }

    @Test
    @DisplayName("Should get employee by ID successfully")
    void getEmployeeById_Success() {

        ApiResponse<Employee> apiResponse = new ApiResponse<>();
        apiResponse.setData(testEmployee);
        apiResponse.setStatus("success");
        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        String expectedUrl = "http://test-domain.com/api/v1/employee/" + employeeId;
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Employee result = employeeService.getEmployeeById(employeeId);

        assertNotNull(result);
        assertEquals(testEmployee.getEmployeeName(), result.getEmployeeName());
    }

    @Test
    @DisplayName("Should throw EmployeeNotFoundException when employee not found")
    void getEmployeeById_NotFound_ThrowsException() {

        String employeeId = UUID.randomUUID().toString();
        String expectedUrl = "http://test-domain.com/api/v1/employee/" + employeeId;
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(EntityNotFoundException.class, () -> employeeService.getEmployeeById(employeeId));
    }

    @Test
    @DisplayName("Should create employee successfully")
    void createEmployee_Success() {

        EmployeeRequest validRequest = new EmployeeRequest("John Doe", 50000, 25, "Developer");

        ApiResponse<Employee> apiResponse = new ApiResponse<>();
        apiResponse.setData(testEmployee);
        apiResponse.setStatus("success");

        ResponseEntity<ApiResponse<Employee>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.CREATED);

        String expectedUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedUrl),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        Employee result = employeeService.createEmployee(validRequest);

        assertNotNull(result);
        assertEquals(testEmployee.getEmployeeName(), result.getEmployeeName());
    }

    @Test
    @DisplayName("Should delete employee successfully")
    void deleteEmployee_Success() {

        ApiResponse<Employee> getResponse = new ApiResponse<>();
        getResponse.setData(testEmployee);
        getResponse.setStatus("success");
        ResponseEntity<ApiResponse<Employee>> getResponseEntity = new ResponseEntity<>(getResponse, HttpStatus.OK);

        ApiResponse<Boolean> deleteResponse = new ApiResponse<>();
        deleteResponse.setData(true);
        deleteResponse.setStatus("success");
        ResponseEntity<ApiResponse<Boolean>> deleteResponseEntity = new ResponseEntity<>(deleteResponse, HttpStatus.OK);

        String expectedGetUrl = "http://test-domain.com/api/v1/employee/" + employeeId;
        when(restTemplate.exchange(
                        eq(expectedGetUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(getResponseEntity);

        String expectedDeleteUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedDeleteUrl),
                        eq(HttpMethod.DELETE),
                        any(HttpEntity.class),
                        any(ParameterizedTypeReference.class)))
                .thenReturn(deleteResponseEntity);

        String result = employeeService.deleteEmployeeById(employeeId);

        assertNotNull(result);
        assertEquals(testEmployee.getEmployeeName(), result);
    }

    @Test
    @DisplayName("Should get top 10 highest earning employee names")
    void getTopTenHighestEarningEmployeeNames_Success() {

        Employee highEarner1 = new Employee(UUID.randomUUID().toString(), "Alice", 100000, 30, "", "");
        Employee highEarner2 = new Employee(UUID.randomUUID().toString(), "Bob", 95000, 28, "", "");
        Employee lowEarner = new Employee(UUID.randomUUID().toString(), "Charlie", 50000, 25, "", "");

        List<Employee> employees = Arrays.asList(highEarner1, highEarner2, lowEarner);
        ApiResponse<List<Employee>> apiResponse = new ApiResponse<>();
        apiResponse.setData(employees);
        apiResponse.setStatus("success");
        ResponseEntity<ApiResponse<List<Employee>>> responseEntity = new ResponseEntity<>(apiResponse, HttpStatus.OK);

        String expectedUrl = "http://test-domain.com/api/v1/employee";
        when(restTemplate.exchange(
                        eq(expectedUrl), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        List<String> result = employeeService.getTopTenHighestEarningEmployeeNames();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("Alice", result.get(0));
        assertEquals("Bob", result.get(1));
        assertEquals("Charlie", result.get(2));
    }
}
