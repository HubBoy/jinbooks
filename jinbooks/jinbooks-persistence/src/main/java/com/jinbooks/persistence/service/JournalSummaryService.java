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

import com.baomidou.mybatisplus.extension.service.IService;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.journal.JournalSummary;
import com.jinbooks.entity.journal.dto.JournalSummaryDto;
import com.jinbooks.entity.journal.dto.JournalSummaryPageDto;
import com.jinbooks.entity.journal.vo.JournalSummaryVo;

public interface JournalSummaryService extends IService<JournalSummary> {
	
    Message<JournalSummaryVo> pageList(JournalSummaryPageDto dto);
    
    //public Message<List <JournalSummary> > findAll() ;
    
    Message<String> delete(ListIdsDto dto);
    
    public Message<String> summaryAccount(JournalSummaryDto dto);
    
}
