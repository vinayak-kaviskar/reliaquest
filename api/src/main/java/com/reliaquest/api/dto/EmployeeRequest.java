package com.reliaquest.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRequest {

    @NotBlank(message = "Employee name cannot be blank")
    private String name;

    @NotNull(message = "Employee salary cannot be null") @Min(value = 1, message = "Employee salary must be greater than zero")
    private Integer salary;

    @NotNull(message = "Employee age cannot be null") @Min(value = 16, message = "Employee age must be at least 16")
    @Max(value = 75, message = "Employee age must be at most 75")
    private Integer age;

    @NotBlank(message = "Employee title cannot be blank")
    private String title;
}
