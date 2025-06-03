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
import com.jinbooks.entity.dto.ChangeStatusDto;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRuleTemplate;
import com.jinbooks.entity.hr.dto.EmployeeSalaryVoucherRulePageDto;
import com.jinbooks.entity.hr.dto.SalaryTemplateDto;
import com.jinbooks.entity.hr.vo.SalaryTemplateVo;
import com.jinbooks.entity.idm.UserInfo;
import com.jinbooks.persistence.service.EmployeeSalaryVoucherRuleService;
import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/11 10:49
 */

@RestController
@RequestMapping("/employee/salary-voucher-rule")
@Slf4j
@RequiredArgsConstructor
public class EmployeeSalaryVoucherRuleController {

    static final Logger logger = LoggerFactory.getLogger(EmployeeSalaryVoucherRuleController.class);

    private final EmployeeSalaryVoucherRuleService employeeSalaryVoucherRuleService;

    @GetMapping(value = {"/fetch"})
    public Message<Page<EmployeeSalaryVoucherRuleTemplate>> fetch(@ParameterObject EmployeeSalaryVoucherRulePageDto dto, @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        logger.debug("fetch {}", dto);
        return employeeSalaryVoucherRuleService.pageList(dto);
    }

    @GetMapping("/get/{id}")
    public Message<SalaryTemplateVo> getById(@PathVariable(name = "id") String id) {
        return employeeSalaryVoucherRuleService.getTemplateDetail(id);
    }

    @PostMapping("/save")
    public Message<String> save(@Validated(value = AddGroup.class) @RequestBody SalaryTemplateDto dto,
                                @CurrentUser UserInfo currentUser) {
        dto.setBookId(currentUser.getBookId());
        return employeeSalaryVoucherRuleService.save(dto);
    }

    @PutMapping("/update")
    public Message<String> update(@Validated(value = EditGroup.class) @RequestBody SalaryTemplateDto dto) {
        return employeeSalaryVoucherRuleService.update(dto);
    }

    @DeleteMapping("/delete")
    public Message<String> delete(@RequestBody ListIdsDto dto) {
        return employeeSalaryVoucherRuleService.delete(dto);
    }

    @PutMapping("/change-status")
    public Message<String> changeStatus(@RequestBody ChangeStatusDto dto) {
        return employeeSalaryVoucherRuleService.changeStatus(dto);
    }
}
