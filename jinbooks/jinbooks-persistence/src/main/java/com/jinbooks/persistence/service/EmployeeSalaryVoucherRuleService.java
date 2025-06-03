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
 

package com.jinbooks.persistence.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.dto.ChangeStatusDto;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRule;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRuleTemplate;
import com.jinbooks.entity.hr.dto.EmployeeSalaryVoucherRulePageDto;
import com.jinbooks.entity.hr.dto.SalaryTemplateDto;
import com.jinbooks.entity.hr.vo.SalaryTemplateVo;

public interface EmployeeSalaryVoucherRuleService extends IService<EmployeeSalaryVoucherRule> {

    Message<Page<EmployeeSalaryVoucherRuleTemplate>> pageList(EmployeeSalaryVoucherRulePageDto dto);

    Message<String> save(SalaryTemplateDto dto);

    Message<String> update(SalaryTemplateDto dto);

    Message<String> delete(ListIdsDto dto);

    Message<String> changeStatus(ChangeStatusDto dto);

    Message<SalaryTemplateVo> getTemplateDetail(String id);
}
