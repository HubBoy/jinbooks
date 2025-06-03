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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/5/6 17:22
 */

@Data
public class SalaryTemplateDto {

    /**
     * 模板ID
     */
    private String id;

    @NotNull(message = "凭证类型不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer voucherType;

    @NotEmpty(message = "凭证字不能为空", groups = {AddGroup.class, EditGroup.class})
    private String wordHead;

    private String bookId;

    @Valid
    @NotEmpty(message = "分录数据不能为空", groups = {AddGroup.class, EditGroup.class})
    List<SalaryTemplateDetailDto> salaryTemplateDetailDtos;
}
