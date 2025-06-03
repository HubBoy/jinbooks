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
import lombok.Data;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/5/6 17:23
 */

@Data
public class SalaryTemplateDetailDto {
    private String id;

    @NotEmpty(message = "摘要不能为空", groups = {AddGroup.class, EditGroup.class})
    private String summary;

    @NotEmpty(message = "借贷方向不能为空", groups = {AddGroup.class, EditGroup.class})
    private String direction;

    @NotEmpty(message = "科目不能为空", groups = {AddGroup.class, EditGroup.class})
    private String subjectId;

    @NotEmpty(message = "取值不能为空", groups = {AddGroup.class, EditGroup.class})
    private String selectedValue;
}
