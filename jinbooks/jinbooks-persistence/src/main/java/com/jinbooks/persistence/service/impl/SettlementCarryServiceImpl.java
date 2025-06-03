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
 

package com.jinbooks.persistence.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.book.Book;
import com.jinbooks.entity.book.BookSubject;
import com.jinbooks.entity.book.Settlement;
import com.jinbooks.entity.book.SettlementCarryforward;
import com.jinbooks.entity.book.vo.SettlementCarryforwardVo;
import com.jinbooks.entity.voucher.VoucherTemplate;
import com.jinbooks.entity.voucher.VoucherTemplateItem;
import com.jinbooks.entity.voucher.dto.GenerateVoucherDto;
import com.jinbooks.entity.voucher.dto.VoucherChangeDto;
import com.jinbooks.entity.voucher.dto.VoucherItemChangeDto;
import com.jinbooks.entity.voucher.dto.VoucherTemplatePageDto;
import com.jinbooks.enums.VoucherStatusEnum;
import com.jinbooks.persistence.mapper.BookMapper;
import com.jinbooks.persistence.mapper.SettlementCarryforwardMapper;
import com.jinbooks.persistence.mapper.SettlementMapper;
import com.jinbooks.persistence.mapper.VoucherTemplateItemMapper;
import com.jinbooks.persistence.mapper.VoucherTemplateMapper;
import com.jinbooks.persistence.service.*;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class SettlementCarryServiceImpl extends ServiceImpl<SettlementMapper, Settlement> implements SettlementCarryService {
	private static final Logger logger = LoggerFactory.getLogger(SettlementCarryServiceImpl.class);

	@Autowired
    IdentifierGenerator identifierGenerator;

    @Autowired
    BookMapper bookMapper;

    @Autowired
    BookSubjectService bookSubjectService;

    @Autowired
    ConfigSysService configSysService;

    @Autowired
    VoucherService voucherService;
    
    @Autowired
    VoucherTemplateService voucherTemplateService;

    @Autowired
    VoucherTemplateItemMapper voucherTemplateItemMapper;

    @Autowired
    VoucherTemplateMapper voucherTemplateMapper;

    @Autowired
    SettlementCarryforwardMapper settlementCarryforwardMapper;

	public Message<Page<SettlementCarryforwardVo>> fetchCarry(VoucherTemplatePageDto dto) {
		dto.setCategory(1);//期末处理模板
        Page<SettlementCarryforwardVo> page = settlementCarryforwardMapper.pageList(dto.build(), dto);
        if(page.getTotal() <= 0) {
        	Book  book = bookMapper.selectById(dto.getRelatedId());
        	voucherTemplateService.insertBookTemplate(dto.getRelatedId(),book.getStandardId());
            //重新查询数据
            page = settlementCarryforwardMapper.pageList(dto.build(), dto);
        }

		return Message.ok(page);
	}


	@Override
	public Message<String> generateVoucher(GenerateVoucherDto dto) {
		logger.debug("GenerateVoucherDto {}",dto);
		String bookId = dto.getBookId();
		Book book = bookMapper.selectById(bookId);
		String currentTerm = configSysService.getCurrentTerm(bookId);
		VoucherTemplate voucherTemplate = voucherTemplateMapper.selectById(dto.getTemplateId());
		logger.debug("voucherTemplate {}",voucherTemplate);
		LambdaQueryWrapper<VoucherTemplateItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(VoucherTemplateItem::getRelatedId, voucherTemplate.getRelatedId());
        itemLqw.eq(VoucherTemplateItem::getTemplateId, voucherTemplate.getId());
        List<VoucherTemplateItem> items = voucherTemplateItemMapper.selectList(itemLqw);
        logger.debug("VoucherTemplateItems {}",items);

        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        Date monthEndDate = configSysService.getCurrentTermLastDate(bookId);
        int year = Integer.parseInt(currentTerm.split("-")[0]);
        int month = Integer.parseInt(currentTerm.split("-")[1]);

        VoucherChangeDto voucherChangeDto = createVoucherChangeDto(book, bookId,voucherTemplate.getWordHead(), monthEndDate, year, month, debitAmount);
        voucherChangeDto.setRemark(voucherTemplate.getRemark().replace("{yyyy}", year+"").replace("{mm}", month+""));

        List<VoucherItemChangeDto> voucherItems = new ArrayList<>();

        if(voucherTemplate.getCode().startsWith("qm_jz_")) {
        	Map<String,VoucherTemplateItem>itemsMap = new HashMap<>();
        	for(VoucherTemplateItem item : items) {
        		itemsMap.put(item.getSubjectCode(), item);
        	}
        	//凭证 不转结
        	voucherChangeDto.setCarryForward("y");
	 
	        if(voucherTemplate.getCode().equals("qm_jz_sr")){//结转收入
	        	//贷
	        	//主营业务收入
	        	addVoucherItems(bookId,"6001",voucherItems,itemsMap.get("6001"));
	        	//其他业务收入
	        	addVoucherItems(bookId,"6301",voucherItems,itemsMap.get("6301"));
	        	//营业外收入
	        	addVoucherItems(bookId,"6051",voucherItems,itemsMap.get("6051"));
	        	for(VoucherItemChangeDto vt :voucherItems) {
	        		debitAmount = debitAmount.add(vt.getDebitAmount());
	        	}
	        	creditAmount = debitAmount ;
	        	//借
	        	//本年利润
	        	voucherItems.add(createVoucherItemDto(bookId,itemsMap.get("4103"),debitAmount));
	        }else if(voucherTemplate.getCode().equals("qm_jz_cbfy")){//结转成本
	        	//主营业务成本
	        	addVoucherItems(bookId,"6401",voucherItems,itemsMap.get("6401"));
	        	//营业税金及附加
	        	addVoucherItems(bookId,"6405",voucherItems,itemsMap.get("6405"));
	        	//销售费用
	        	addVoucherItems(bookId,"6601",voucherItems,itemsMap.get("6601"));
	        	//管理费用
	        	addVoucherItems(bookId,"6602",voucherItems,itemsMap.get("6602"));
	        	//财务费用
	        	addVoucherItems(bookId,"6603",voucherItems,itemsMap.get("6603"));
	        	//营业外支出
	        	addVoucherItems(bookId,"6711",voucherItems,itemsMap.get("6711"));
	        	for(VoucherItemChangeDto vt :voucherItems) {
	        		creditAmount = creditAmount.add(vt.getCreditAmount());
	        	}
	        	debitAmount = creditAmount;
	        	//本年利润
	        	voucherItems.add(createVoucherItemDto(bookId,itemsMap.get("4103"),debitAmount));
	        }else if(voucherTemplate.getCode().equals("qm_jz_sds")){//结转所得税
	        	//所得税
	        	addVoucherItems(bookId,"6801",voucherItems,itemsMap.get("6801"));
	        	for(VoucherItemChangeDto vt :voucherItems) {
	        		creditAmount = creditAmount.add(vt.getCreditAmount());
	        	}
	        	debitAmount = creditAmount;
	        	//本年利润
	        	voucherItems.add(createVoucherItemDto(bookId,itemsMap.get("4103"),debitAmount));
	        }else if(voucherTemplate.getCode().equals("qm_jz_bnlr")){//年末 结转本年利润
	        	if(month == 12) {
		        	//本年利润
		        	BookSubject bnlrSubject = bookSubjectService.selectSubject(bookId,"4103");
		        	voucherItems.add(createVoucherItemDto(bookId,itemsMap.get("4103"),bnlrSubject.getBalance()));
		        	//未分配利润
		        	BookSubject wfplrSubject = bookSubjectService.selectSubject(bookId,"410406");
		        	voucherItems.add(createVoucherItemDto(bookId,itemsMap.get("410406"),wfplrSubject.getBalance()));
	        
	        	}else {
	        		return Message.failed("非年末，无需结转本年利润");
	        	}
	        }
        }else {
        	for(VoucherTemplateItem item : items) {
        		voucherItems.add(createVoucherItemDto(bookId,item,BigDecimal.ZERO));
        	}
        }

        voucherChangeDto.setItems(voucherItems);
        //草稿阶段
        voucherChangeDto.setStatus(VoucherStatusEnum.DRAFT.getValue());
        log.debug("voucherChangeDto {}",voucherChangeDto);
        //保持凭证
        voucherService.save(voucherChangeDto);

        //结转记录
        SettlementCarryforward settlementCarryforward = new SettlementCarryforward();
        settlementCarryforward.setBookId(bookId);
        settlementCarryforward.setYear(year);
        settlementCarryforward.setYearPeriod(currentTerm);
        settlementCarryforward.setVoucherId(voucherChangeDto.getId());
        settlementCarryforward.setVoucherTemplateId(voucherTemplate.getId());
        //保存结转记录
        settlementCarryforwardMapper.insert(settlementCarryforward);
        //返回凭证ID编码
		return Message.ok(voucherChangeDto.getId());
	}
	
	private boolean addVoucherItems(String bookId,String subjectCode,List<VoucherItemChangeDto> items,VoucherTemplateItem templateItem) {
		List<BookSubject> subjectList = bookSubjectService.selectSubjectAndChild(bookId, subjectCode);
		for(BookSubject s : subjectList ) {
			if(isLeafSubject(s,subjectList) && s.getBalance().compareTo(BigDecimal.ZERO) > 0) {
				items.add(createVoucherItemDtoBySubject(bookId,s,templateItem,s.getBalance()));
			}
		}
		return true;
	}
	
	private boolean isLeafSubject(BookSubject subject,List<BookSubject> subjectList) {
		boolean isLeaf = true;
		//仅有一条数据
		if(subjectList.size() == 1) {
			return true;
		}
		//多条数据
		for(BookSubject s : subjectList ) {
			//跳过自己
			if(subject.getCode().equals(s.getCode())) {
				continue;
			}
			//有节点以当前节点开头认为不是叶节点
			if(s.getCode().startsWith(subject.getCode())) {
				isLeaf = false;
				break;
			}
		}
		return isLeaf;
	}


	/**
     * Creates the voucher change dto with common fields
     */
    private VoucherChangeDto createVoucherChangeDto(Book book, String bookId,String wordHead,
                                                    Date voucherDate, Integer year, Integer month, BigDecimal amount) {

        Integer wordNum = voucherService.getAbleWordNum(bookId, wordHead, null, null).getData();

        VoucherChangeDto dto = new VoucherChangeDto();
        dto.setWordHead(wordHead);
        dto.setWordNum(wordNum);
        dto.setBookId(bookId);
        dto.setCompanyName(book.getCompanyName());
        dto.setVoucherDate(voucherDate);
        dto.setVoucherYear(year);
        dto.setVoucherMonth(month);
        dto.setDebitAmount(amount);
        dto.setCreditAmount(amount);

        return dto;
    }

    /**
     * Creates a voucher item dto based on rule and direction
     */
    private VoucherItemChangeDto createVoucherItemDto(String bookId,
    		VoucherTemplateItem item, BigDecimal amount) {
        BookSubject bookSubject = bookSubjectService.selectSubject(bookId,item.getSubjectCode());

        VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
        itemDto.setSummary(item.getSummary());
        itemDto.setSubjectId(bookSubject.getId());
        if (item.getDirection() == 1) {
            itemDto.setDebitAmount(amount);
        } else {
            itemDto.setCreditAmount(amount);
        }
        itemDto.setSubjectBalance(bookSubject.getBalance());
        itemDto.setAuxiliary(List.of());
        itemDto.setSubjectCode(bookSubject.getCode());
        itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
        itemDto.setDetailedAccounts("");

        return itemDto;
    }
    
    /**
     * Creates a voucher item dto based on rule and direction
     */
    private VoucherItemChangeDto createVoucherItemDtoBySubject(String bookId,BookSubject bookSubject,
    		VoucherTemplateItem item, BigDecimal amount) {
        VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
        itemDto.setSummary(item.getSummary());
        itemDto.setSubjectId(bookSubject.getId());
        if (item.getDirection() == 1) {
            itemDto.setDebitAmount(amount);
        } else {
            itemDto.setCreditAmount(amount);
        }
        itemDto.setSubjectBalance(bookSubject.getBalance());
        itemDto.setAuxiliary(List.of());
        itemDto.setSubjectCode(bookSubject.getCode());
        itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
        itemDto.setDetailedAccounts("");

        return itemDto;
    }


	@Override
	public Message<String> delete(String bookId, String voucherId) {
		LambdaQueryWrapper<SettlementCarryforward> carryLqw = Wrappers.lambdaQuery();
		carryLqw.eq(SettlementCarryforward::getBookId, bookId);
		carryLqw.eq(SettlementCarryforward::getVoucherId, voucherId);
		SettlementCarryforward settlementCarryforward = settlementCarryforwardMapper.selectOne(carryLqw);
		ArrayList<String> voucherIds = new ArrayList<String>();
		voucherIds.add(settlementCarryforward.getVoucherId());
		voucherService.delete(voucherIds, bookId);
		
		settlementCarryforwardMapper.delete(carryLqw);
		return null;
	}

}
