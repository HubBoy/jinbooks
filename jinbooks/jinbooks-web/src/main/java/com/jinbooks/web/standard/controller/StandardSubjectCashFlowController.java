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
 

package com.jinbooks.web.standard.controller;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.idm.UserInfo;
import com.jinbooks.entity.standard.dto.StandardSubjectCashFlowDto;
import com.jinbooks.entity.standard.vo.StandardSubjectCashFlowVo;
import com.jinbooks.persistence.service.StandardSubjectCashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/4/21 10:43
 */

@RestController
@RequestMapping("/config/subject-cash-flow")
@Slf4j
@RequiredArgsConstructor
public class StandardSubjectCashFlowController {
    private final StandardSubjectCashFlowService standardSubjectCashFlowService;

    @PostMapping(value = {"/save"})
    public Message<String> save(@Validated @RequestBody StandardSubjectCashFlowDto dto,
                                                 @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return standardSubjectCashFlowService.save(dto);
    }

    @GetMapping("/fetch-relationships")
    public Message<StandardSubjectCashFlowVo> fetchRelationships(@ParameterObject StandardSubjectCashFlowDto dto,
                                                                 @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        return standardSubjectCashFlowService.fetchRelationships(dto);
    }
}
