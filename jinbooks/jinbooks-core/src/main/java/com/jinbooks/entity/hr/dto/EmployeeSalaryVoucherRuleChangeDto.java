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
 

package com.jinbooks.entity.hr.dto;

import com.jinbooks.validate.AddGroup;
import com.jinbooks.validate.EditGroup;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/11 15:12
 */

@Data
public class EmployeeSalaryVoucherRuleChangeDto {
    @NotEmpty(message = "编辑对象不能为空", groups = {EditGroup.class})
    private String id;

    @NotEmpty(message = "摘要不能为空", groups = {AddGroup.class, EditGroup.class})
    private String summary;

    @NotEmpty(message = "借贷方向不能为空", groups = {AddGroup.class, EditGroup.class})
    private String direction;

    @NotEmpty(message = "科目不能为空", groups = {AddGroup.class, EditGroup.class})
    private String subjectId;

    @NotNull(message = "凭证类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer voucherType;

    @NotEmpty(message = "凭证字不能为空", groups = {AddGroup.class, EditGroup.class})
    private String wordHead;

    @NotNull(message = "状态不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer status;

    private String bookId;
}
