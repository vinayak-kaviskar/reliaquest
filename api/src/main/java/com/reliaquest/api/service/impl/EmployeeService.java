package com.reliaquest.api.service.impl;

import com.reliaquest.api.dto.ApiResponse;
import com.reliaquest.api.dto.EmployeeRequest;
import com.reliaquest.api.exception.EntityNotFoundException;
import com.reliaquest.api.exception.ExternalServiceException;
import com.reliaquest.api.exception.RequestValidationException;
import com.reliaquest.api.exception.TooManyRequestsException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeeName;
import com.reliaquest.api.service.IEmployeeService;
import jakarta.annotation.PreDestroy;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class EmployeeService implements IEmployeeService {

    private final RestTemplate restTemplate;
    private final Validator validator;
    private final ValidatorFactory validatorFactory;

    @Value("${app.employee-service.domain}")
    private String domain;

    @Value("${app.employee-service.base-path}")
    private String basePath;

    @Autowired
    public EmployeeService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.validatorFactory = Validation.buildDefaultValidatorFactory();
        this.validator = validatorFactory.getValidator();
    }

    private static void validateId(String id) {

        if (id == null || id.trim().isEmpty()) {
            log.warn("Employee ID cannot be null or empty");
            throw new IllegalArgumentException("Employee ID cannot be null or empty");
        }

        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID format for employee id: {}", id);
            throw new IllegalArgumentException("Invalid employee ID format. Expected a valid UUID.", e);
        }
    }

    private String buildUrl(String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(domain)
                .path(basePath)
                .pathSegment(pathSegments)
                .toUriString();
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public List<Employee> getAllEmployees() {
        log.info("Fetching all employees from external service");

        try {
            ResponseEntity<ApiResponse<List<Employee>>> response = restTemplate.exchange(
                    buildUrl(), HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {});

            if (response == null || response.getBody() == null) {
                log.error("Received null response from external service");
                throw new ExternalServiceException("Invalid response from external service");
            }

            ApiResponse<List<Employee>> apiResponse = response.getBody();
            List<Employee> employees = apiResponse.getData();

            if (employees == null) {
                log.warn("External service returned null employee data");
                return Collections.emptyList();
            }

            log.info("Successfully retrieved {} employees from external service", employees.size());
            return employees;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.info("Retrying get all employees");
                throw new TooManyRequestsException("Too many requests, retrying...");
            }
            log.error("Error fetching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve employees from external service", e);
        } catch (RestClientException e) {
            log.error("Failed to communicate with external service", e);
            throw new ExternalServiceException("Failed to retrieve employees from external service", e);
        }
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public Employee getEmployeeById(String id) {
        log.info("Fetching employee with id: {}", id);

        validateId(id);

        try {
            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    buildUrl(id), HttpMethod.GET, null, new ParameterizedTypeReference<ApiResponse<Employee>>() {});

            if (response == null || response.getBody() == null) {
                log.error("Received null response from external service for employee id: {}", id);
                throw new ExternalServiceException("Invalid response from external service");
            }

            ApiResponse<Employee> apiResponse = response.getBody();
            Employee employee = apiResponse.getData();

            log.info("Successfully retrieved employee with id: {}", id);
            return employee;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.info("Retrying get employee by ID");
                throw new TooManyRequestsException("Too many requests, retrying...");
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Employee not found with id: {}", id);
                throw new EntityNotFoundException("Employee not found with id: " + id);
            }
            log.error("Error fetching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to retrieve employee by Id from external service", e);
        } catch (RestClientException e) {
            log.error("Failed to fetch employee with id: {}", id, e);
            throw new ExternalServiceException("Failed to retrieve employee from external service", e);
        }
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public List<Employee> getEmployeesByNameSearch(String searchString) {
        log.info("Searching employees with name containing: {}", searchString);

        List<Employee> allEmployees = getAllEmployees();

        List<Employee> filteredEmployees = allEmployees.stream()
                .filter(employee -> employee.getEmployeeName() != null
                        && employee.getEmployeeName().toLowerCase().contains(searchString.toLowerCase()))
                .collect(Collectors.toList());

        log.info("Found {} employees matching search criteria: {}", filteredEmployees.size(), searchString);
        return filteredEmployees;
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public Integer getHighestSalaryOfEmployees() {
        log.info("Fetching highest salary among all employees");

        List<Employee> allEmployees = getAllEmployees();

        if (allEmployees.isEmpty()) {
            log.warn("No employees found to determine highest salary");
            return 0;
        }

        Integer highestSalary = allEmployees.stream()
                .filter(employee -> employee.getEmployeeSalary() != null)
                .mapToInt(Employee::getEmployeeSalary)
                .max()
                .orElse(0);

        log.info("Highest salary found: {}", highestSalary);
        return highestSalary;
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public List<String> getTopTenHighestEarningEmployeeNames() {
        log.info("Fetching top 10 highest earning employee names");

        List<Employee> allEmployees = getAllEmployees();

        List<String> topTenNames = allEmployees.stream()
                .filter(employee -> employee.getEmployeeSalary() != null)
                .sorted(Comparator.comparing(Employee::getEmployeeSalary).reversed())
                .limit(10)
                .map(Employee::getEmployeeName)
                .collect(Collectors.toList());

        log.info("Retrieved {} top earning employee names", topTenNames.size());
        return topTenNames;
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public Employee createEmployee(EmployeeRequest employeeRequest) {
        log.info("Creating new employee: {}", employeeRequest.getName());

        validateReqBody(employeeRequest);

        try {
            HttpEntity<EmployeeRequest> requestEntity = new HttpEntity<>(employeeRequest);

            ResponseEntity<ApiResponse<Employee>> response = restTemplate.exchange(
                    buildUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<ApiResponse<Employee>>() {});

            if (response == null || response.getBody() == null) {
                log.error("Received null response from external service while creating employee");
                throw new ExternalServiceException("Invalid response from external service");
            }

            ApiResponse<Employee> apiResponse = response.getBody();
            Employee createdEmployee = apiResponse.getData();

            if (createdEmployee == null) {
                log.error("Failed to create employee: {}", employeeRequest.getName());
                throw new ExternalServiceException("Failed to create employee");
            }

            log.info("Successfully created employee with id: {}", createdEmployee.getId());
            return createdEmployee;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.info("Retrying create employee");
                throw new TooManyRequestsException("Too many requests, retrying...");
            }
            log.error("Error fetching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to create employee in external service", e);
        } catch (RestClientException e) {
            log.error("Failed to create employee: {}", employeeRequest.getName(), e);
            throw new ExternalServiceException("Failed to create employee in external service", e);
        }
    }

    @Override
    @Retryable(
            retryFor = {TooManyRequestsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.retry.initial-delay}",
                            multiplierExpression = "${app.retry.multiplier}"))
    public String deleteEmployeeById(String id) {
        log.info("Deleting employee with id: {}", id);

        // First get the employee to retrieve the name
        Employee employee = getEmployeeById(id);
        String employeeName = employee.getEmployeeName();
        EmployeeName empDelReq = EmployeeName.builder().name(employeeName).build();
        HttpEntity<EmployeeName> deleteRequest = new HttpEntity<>(empDelReq);

        try {
            ResponseEntity<ApiResponse<Boolean>> response = restTemplate.exchange(
                    buildUrl(),
                    HttpMethod.DELETE,
                    deleteRequest,
                    new ParameterizedTypeReference<ApiResponse<Boolean>>() {});

            if (response == null || response.getBody() == null) {
                log.error("Received null response from external service while deleting employee");
                throw new ExternalServiceException("Invalid response from external service");
            }

            ApiResponse<Boolean> apiResponse = response.getBody();
            Boolean isDeleted = apiResponse.getData();

            if (isDeleted == null || !isDeleted) {
                log.error("Failed to delete employee with id: {}", id);
                throw new ExternalServiceException("Failed to delete employee");
            }

            log.info("Successfully deleted employee: {}", employeeName);
            return employeeName;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.info("Retrying delete employee by ID");
                throw new TooManyRequestsException("Too many requests, retrying...");
            }
            log.error("Error fetching employees: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to delete employee from external service", e);
        } catch (RestClientException e) {
            log.error("Failed to delete employee with id: {}", id, e);
            throw new ExternalServiceException("Failed to delete employee from external service", e);
        }
    }

    private void validateReqBody(EmployeeRequest employeeRequest) {
        Set<ConstraintViolation<EmployeeRequest>> violations = validator.validate(employeeRequest);
        if (!violations.isEmpty()) {
            List<String> errorMessages = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.toList());
            throw new RequestValidationException("Validation failed", errorMessages);
        }
    }

    @PreDestroy
    public void close() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }
}
