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

import com.jinbooks.entity.Message;
import com.jinbooks.entity.book.Settlement;
import com.jinbooks.entity.book.dto.BookChangeDto;
import com.jinbooks.entity.statement.StatementBalanceSheet;
import com.jinbooks.entity.statement.StatementBalanceSheetItem;
import com.jinbooks.entity.statement.StatementRules;
import com.jinbooks.entity.statement.StatementSubjectBalance;
import com.jinbooks.entity.statement.dto.StatementParamsDto;
import com.jinbooks.entity.statement.vo.StatementBalanceSheetItemListVo;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 财务报表业务接口
 */
public interface StatementBalanceSheetService {

    /**
     * 获取资产负载报表数据
     *
     * @param dto   查询参数
     * @param force 是否实时统计
     * @return 结果
     */
    Message<StatementBalanceSheet> queryBalanceSheet(StatementParamsDto dto, boolean force);

    Message<StatementBalanceSheet> balanceSheet(StatementParamsDto dto, boolean save);
    
    boolean deleteByBookIds(List<String> bookIds);

    void initBalanceSheet(BookChangeDto dto);
    
    boolean checkout(Settlement dto) ;

    void export(StatementParamsDto dto, HttpServletResponse response) throws IOException;

    void updateRuleBalance(StatementSubjectBalance subjectBalance, StatementRules statementRules);

    void refreshItemsBalance(List<StatementBalanceSheetItem> items,
                             String bookId, String yearPeriod, int updateParams);

    StatementBalanceSheetItemListVo insertSubtotals(List<StatementBalanceSheetItem> items);
    
}
