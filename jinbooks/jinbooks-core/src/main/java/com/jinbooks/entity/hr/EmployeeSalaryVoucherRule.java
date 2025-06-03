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
 

package com.jinbooks.entity.hr;

import com.baomidou.mybatisplus.annotation.*;
import com.jinbooks.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/11 10:40
 */

@EqualsAndHashCode(callSuper = true)
@TableName("jbx_employee_salary_voucher_rule")
@Data
public class EmployeeSalaryVoucherRule extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -1147383342734834415L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String summary;

    private String direction;

    private String subjectId;

    private String templateId;

    private String selectedValue;

    @TableField(exist = false)
    private String subjectName;

    @TableField(exist = false)
    private String subjectCode;
}
