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
 

package com.jinbooks.web.statement.controller;

import com.jinbooks.authn.annotation.CurrentUser;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.idm.UserInfo;
import com.jinbooks.entity.statement.StatementCashFlow;
import com.jinbooks.entity.statement.StatementSubjectBalance;
import com.jinbooks.entity.statement.dto.StatementParamsDto;
import com.jinbooks.exception.ServiceException;
import com.jinbooks.persistence.service.StatementReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 各类报表接口
 */

@RestController
@RequestMapping("/statement")
@Slf4j
@RequiredArgsConstructor
public class StatementReportController {
    private final StatementReportService statementReportService;

    /**
     * 报表-现金流量表
     *
     * @param dto 查询参数
     * @return 结果
     */
    @GetMapping(value = {"/cash-flow"})
    public Message<List<StatementCashFlow>> cashFlow(@ParameterObject StatementParamsDto dto,
                                                                   @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.cashFlowStatement(dto);
    }

    /**
     * 现金流量表导出功能
     */
    @GetMapping("/cash-flow/export")
    public void cashFlowExport(HttpServletResponse response,
                       @ParameterObject StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.cashFlowExport(dto, response);
    }

    /**
     * 报表-科目余额表
     *
     * @param dto 查询参数
     * @return 结果
     */
    @GetMapping(value = {"/subject-balance"})
    public Message<List<StatementSubjectBalance>> subjectBalance(@ParameterObject StatementParamsDto dto,
                                                               @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.subjectBalance(dto);
    }

    /**
     * 科目余额表导出功能
     */
    @GetMapping("/subject-balance/export")
    public void subjectBalanceExport(HttpServletResponse response,
                       @ParameterObject StatementParamsDto dto,
                       @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.subjectBalanceExport(dto, response);
    }

    /**
     * 报表-凭证汇总表
     *
     * @param dto 查询参数
     * @return 结果
     */
    @GetMapping(value = {"/voucher-summary"})
    public Message<List<StatementSubjectBalance>> voucherSummary(@ParameterObject StatementParamsDto dto,
                                                            @CurrentUser UserInfo userInfo) {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        return statementReportService.voucherSummary(dto);
    }

    /**
     * 凭证汇总表导出功能
     */
    @GetMapping("/voucher-summary/export")
    public void voucherSummaryExport(HttpServletResponse response,
                                     @ParameterObject StatementParamsDto dto,
                                     @CurrentUser UserInfo userInfo) throws IOException {
        dto.setBookId(userInfo.getBookId());
        validParams(dto);
        statementReportService.voucherSummaryExport(dto, response);
    }

    private void validParams(StatementParamsDto dto) {
        if (StringUtils.isEmpty(dto.getPeriodType())) {
            throw new ServiceException("统计类型参数为空");
        } else if (StringUtils.isEmpty(dto.getReportDate())) {
            throw new ServiceException("统计日期参数为空");
        }
    }
}
