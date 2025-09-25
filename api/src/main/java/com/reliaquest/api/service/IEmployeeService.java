package com.reliaquest.api.service;

import com.reliaquest.api.dto.EmployeeRequest;
import com.reliaquest.api.model.Employee;
import java.util.List;

public interface IEmployeeService {
    List<Employee> getAllEmployees();

    Employee getEmployeeById(String id);

    List<Employee> getEmployeesByNameSearch(String searchString);

    Integer getHighestSalaryOfEmployees();

    List<String> getTopTenHighestEarningEmployeeNames();

    Employee createEmployee(EmployeeRequest employeeRequest);

    String deleteEmployeeById(String id);
}
