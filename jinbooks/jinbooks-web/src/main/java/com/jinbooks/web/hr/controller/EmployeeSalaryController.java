/*
 * Copyright [2025] [JinBooks of copyright http://www.jinbooks.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.jinbooks.web.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.hr.EmployeeSalary;
import com.jinbooks.entity.hr.EmployeeSalarySummary;
import com.jinbooks.entity.hr.dto.SalaryDetailChangeDto;
import com.jinbooks.entity.hr.dto.SalaryDetailPageDto;
import com.jinbooks.entity.hr.dto.SalarySummaryChangeDto;
import com.jinbooks.entity.idm.UserInfo;
import com.jinbooks.entity.voucher.dto.GenerateVoucherDto;
import com.jinbooks.persistence.service.EmployeeSalaryService;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/20 17:02
 */

@RestController
@RequestMapping("/employee/salary")
@Slf4j
public class EmployeeSalaryController {
    static final Logger logger = LoggerFactory.getLogger(EmployeeSalaryController.class);

    @Autowired
    EmployeeSalaryService employeeSalaryService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeSalary>> fetch(@ParameterObject SalaryDetailPageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        log.debug("fetch {}", dto);
        return employeeSalaryService.pageList(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SalaryDetailChangeDto dto,
                                  @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        logger.debug("-update  {}", dto);
        return employeeSalaryService.update(dto);
    }

    @PutMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SalaryDetailChangeDto dto,
                                  @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        logger.debug("-save  {}", dto);
        return employeeSalaryService.save(dto);
    }

    @GetMapping("/get/{id}")
    public Message<EmployeeSalary> getById(@PathVariable(name = "id") String id) {
        return Message.ok(employeeSalaryService.getById(id));
    }

    @GetMapping("/summary")
    public Message<EmployeeSalarySummary> summary(@ParameterObject SalarySummaryChangeDto dto,@CurrentUser UserInfo currentUser) {
    	if(dto.getBelongDateRange()!= null && dto.getBelongDateRange().length ==2
    			&& dto.getBelongDateRange()[0].equalsIgnoreCase(dto.getBelongDateRange()[1])) {
    		dto.setBelongDate(YearMonth.parse(dto.getBelongDateRange()[0]));
    	}
    	dto.setBookId(currentUser.getBookId());
        return Message.ok(employeeSalaryService.selectSalarySummary(dto));
    }

    @GetMapping("/export")
    public Message<String> exportTaxItems(@ParameterObject SalaryDetailPageDto dto,
            HttpServletResponse response, @CurrentUser UserInfo currentUser) {
            dto.setBookId(currentUser.getBookId());
            return employeeSalaryService.exportTaxItems(dto, response);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return employeeSalaryService.delete(dto);
    }


    @PostMapping("/generate-voucher")
    public Message<String> generateVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryService.generateVoucher(dto);
    }
    
    @PostMapping("/delete-voucher")
    public Message<String> deleteVoucher(@Validated @RequestBody GenerateVoucherDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryService.deleteVoucher(dto);
    }
}
