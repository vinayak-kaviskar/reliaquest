package com.reliaquest.api.unit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.dto.EmployeeRequest;
import com.reliaquest.api.exception.RequestValidationException;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.impl.EmployeeService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
@DisplayName("Employee Controller Unit Tests")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    private Employee employee1;
    private Employee employee2;
    private List<Employee> employeeList;
    private EmployeeRequest validEmployeeRequest;

    @BeforeEach
    void setUp() {
        employee1 = Employee.builder()
                .id("1")
                .employeeName("John Doe")
                .employeeSalary(50000)
                .employeeAge(30)
                .employeeTitle("Developer")
                .employeeEmail("john.doe@company.com")
                .build();

        employee2 = Employee.builder()
                .id("2")
                .employeeName("Jane Smith")
                .employeeSalary(60000)
                .employeeAge(28)
                .employeeTitle("Senior Developer")
                .employeeEmail("jane.smith@company.com")
                .build();

        employeeList = Arrays.asList(employee1, employee2);

        validEmployeeRequest = EmployeeRequest.builder()
                .name("New Employee")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/employee - Should return all employees")
    void getAllEmployees_WhenEmployeesExist_ShouldReturnEmployeeList() throws Exception {

        when(employeeService.getAllEmployees()).thenReturn(employeeList);

        mockMvc.perform(get("/api/v1/employee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("1")))
                .andExpect(jsonPath("$[0].employee_name", is("John Doe")))
                .andExpect(jsonPath("$[1].id", is("2")))
                .andExpect(jsonPath("$[1].employee_name", is("Jane Smith")));
    }

    @Test
    @DisplayName("POST /api/v1/employee - Should create employee with valid request")
    void createEmployee_WhenValidRequest_ShouldCreateEmployee() throws Exception {

        Employee createdEmployee = Employee.builder()
                .id("3")
                .employeeName("New Employee")
                .employeeSalary(70000)
                .employeeAge(25)
                .employeeTitle("Junior Developer")
                .employeeEmail("new.employee@company.com")
                .build();

        when(employeeService.createEmployee(any(EmployeeRequest.class))).thenReturn(createdEmployee);

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validEmployeeRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is("3")))
                .andExpect(jsonPath("$.employee_name", is("New Employee")))
                .andExpect(jsonPath("$.employee_salary", is(70000)));
    }

    @Test
    @DisplayName("POST /api/v1/employee - Should return 400 when name is blank")
    void createEmployee_WhenNameIsBlank_ShouldReturn400() throws Exception {

        EmployeeRequest invalidRequest = EmployeeRequest.builder()
                .name("")
                .salary(70000)
                .age(25)
                .title("Junior Developer")
                .build();

        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenThrow(
                        new RequestValidationException("Name cannot be blank", Arrays.asList("Name cannot be blank")));

        mockMvc.perform(post("/api/v1/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Name cannot be blank"));
    }

    @Test
    @DisplayName("DELETE /api/v1/employee/{id} - Should delete employee and return name")
    void deleteEmployeeById_WhenEmployeeExists_ShouldDeleteAndReturnName() throws Exception {

        String employeeId = "1";
        String employeeName = "John Doe";
        when(employeeService.deleteEmployeeById(employeeId)).thenReturn(employeeName);

        mockMvc.perform(delete("/api/v1/employee/{id}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is("John Doe")));
    }
}
