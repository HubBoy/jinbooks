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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.book.Book;
import com.jinbooks.entity.book.BookSubject;
import com.jinbooks.entity.hr.EmployeeSalarySummary;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRule;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRuleTemplate;
import com.jinbooks.entity.hr.dto.SalaryDetailPageDto;
import com.jinbooks.entity.hr.dto.SalarySummaryChangeDto;
import com.jinbooks.entity.voucher.dto.GenerateVoucherDto;
import com.jinbooks.entity.voucher.dto.VoucherChangeDto;
import com.jinbooks.entity.voucher.dto.VoucherItemChangeDto;
import com.jinbooks.enums.SalaryVoucherTemplateEnum;
import com.jinbooks.enums.VoucherStatusEnum;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.persistence.mapper.*;
import com.jinbooks.persistence.service.ConfigSysService;
import com.jinbooks.persistence.service.EmployeeSalarySummaryService;
import com.jinbooks.persistence.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/2/27 17:45
 */

@Service

public class EmployeeSalarySummaryServiceImpl extends ServiceImpl<EmployeeSalarySummaryMapper, EmployeeSalarySummary> implements EmployeeSalarySummaryService {

    @Autowired
    EmployeeSalaryMapper employeeSalaryMapper;

    @Autowired
    EmployeeSalaryVoucherRuleMapper employeeSalaryVoucherRuleMapper;

    @Autowired
    EmployeeSalaryVoucherRuleTemplateMapper employeeSalaryVoucherRuleTemplateMapper;

    @Autowired
    VoucherService voucherService;

    @Autowired
    BookMapper bookMapper;

    @Autowired
    BookSubjectMapper bookSubjectMapper;

    @Autowired
    ConfigSysService configSysService;

    @Override
    @Transactional
    public Message<String> save(SalarySummaryChangeDto dto) {
        YearMonth lastMonth = YearMonth.parse(configSysService.getCurrentTerm(dto.getBookId()));
        dto.setBelongDate(lastMonth);
        int count = employeeSalaryMapper.countEmployeeSalaries(dto);
        if (count > 0) {
            super.remove(Wrappers.<EmployeeSalarySummary>lambdaQuery()
                    .eq(EmployeeSalarySummary::getBookId, dto.getBookId())
                    .eq(EmployeeSalarySummary::getBelongDate, lastMonth));

            //员工费用
            boolean result =false;
            EmployeeSalarySummary employeeSalarySummary = employeeSalaryMapper.selectSalarySummary(dto);
            if(employeeSalarySummary != null) {
            	result = super.save(employeeSalarySummary);
            }

            //兼职费用
            employeeSalarySummary = employeeSalaryMapper.selectSalarySummaryLabor(dto);
            if(employeeSalarySummary != null) {
            	result = super.save(employeeSalarySummary);
            }
            return result ? Message.ok("成功") : Message.failed("失败");
        }

        return Message.failed("暂无数据，请先计算当月工资然后推送工资明细");
    }

    @Override
    public Message<Page<EmployeeSalarySummary>> pageList(SalaryDetailPageDto dto) {
        LambdaQueryWrapper<EmployeeSalarySummary> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dto.getLabel())) {
            wrapper.like(EmployeeSalarySummary::getLabel, dto.getLabel());
        }
        wrapper.eq(EmployeeSalarySummary::getBookId, dto.getBookId());
        if (ObjectUtils.isNotEmpty(dto.getBelongDateRange()) && dto.getBelongDateRange().length >= 2) {
            String startDate = dto.getBelongDateRange()[0];
            String endDate = dto.getBelongDateRange()[1];

            wrapper.ge(EmployeeSalarySummary::getBelongDate, startDate)
                    .le(EmployeeSalarySummary::getBelongDate, endDate);
        }
        wrapper.orderByDesc(EmployeeSalarySummary::getBelongDate);
        Page<EmployeeSalarySummary> page = super.page(dto.build(), wrapper);
        return Message.ok(page);
    }

    @Override
    @Transactional
    public Message<String> generateVoucher(GenerateVoucherDto dto) {
        String bookId = dto.getBookId();
        Book book = bookMapper.selectById(bookId);
        Integer voucherType = dto.getVoucherType();
        EmployeeSalarySummary summary = super.getById(dto.getId());

        if (voucherType == 0 && StringUtils.isNotBlank(summary.getAccrualVoucherId())) {
            return Message.ok("计提凭证已生成");
        } else if (voucherType == 1 && StringUtils.isNotBlank(summary.getSalaryVoucherId())) {
            return Message.ok("发放凭证已生成");
        } else if (voucherType == 2 && StringUtils.isNotBlank(summary.getAccrualVoucherId())) {
            return Message.ok("收票（兼职）凭证已生成");
        } else if (voucherType == 3 && StringUtils.isNotBlank(summary.getSalaryVoucherId())) {
            return Message.ok("发放（兼职）凭证已生成");
        }

        Date formattedDate = getFormattedCurrentDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formattedDate);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;

        List<EmployeeSalaryVoucherRule> rules = getSalaryVoucherRules(bookId, voucherType);
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal creditAmount = BigDecimal.ZERO;

        List<VoucherItemChangeDto> voucherItems = new ArrayList<>();

        for (EmployeeSalaryVoucherRule rule : rules) {
            rule.setSummary(generateVoucherItemSummary(rule.getSummary(), year, month));

            String selectedValue = rule.getSelectedValue();
            SalaryVoucherTemplateEnum matchedEnum = SalaryVoucherTemplateEnum.fromValue(selectedValue);
            if (matchedEnum == null) {
                throw new BusinessException(50001, "凭证模板明细数据有误，请联系管理员");
            }

            BigDecimal amount = switch (matchedEnum) {
                case COMPANY_COSTS -> summary.getBusinessExpenditureCosts();
                case SALARY_PAYABLE -> summary.getTotalAmount().add(summary.getTotalSocialInsurance()).add(summary.getProvidentFund()).add(summary.getPersonalTax());
                case ACTUAL_SALARY -> summary.getTotalAmount();
                case PERSONAL_INCOME_TAX -> summary.getPersonalTax();
                case PERSONAL_WITHHOLDING_SOCIAL_SECURITY -> summary.getTotalSocialInsurance();
                case PERSONAL_WITHHOLDING_PROVIDENT_FUND -> summary.getProvidentFund();
                case ENTERPRISES_PAY_SOCIAL_INSURANCE -> summary.getBusinessSocialInsurance();
                case PROVIDENT_FUND_PAID_BY_ENTERPRISES -> summary.getBusinessProvidentFund();
            };

            boolean isDebit = "1".equals(rule.getDirection());
            voucherItems.add(createVoucherItemDto(bookId, rule, isDebit, amount));

            if (isDebit) {
                debitAmount = debitAmount.add(amount);
            } else {
                creditAmount = creditAmount.add(amount);
            }
        }

        if (debitAmount.compareTo(creditAmount) != 0) {
            throw new BusinessException(50001, "借贷不平衡，请调整工资凭证模板");
        }

        VoucherChangeDto voucherChangeDto = createVoucherChangeDto(book, bookId, formattedDate, year, month, debitAmount);
        voucherChangeDto.setRemark(rules.get(0).getSummary());
        voucherChangeDto.setItems(voucherItems);
        voucherChangeDto.setStatus(VoucherStatusEnum.DRAFT.getValue());

        voucherService.save(voucherChangeDto);

        LambdaUpdateWrapper<EmployeeSalarySummary> updateWrapper = new LambdaUpdateWrapper<>();
        if (voucherType == 0 || voucherType == 2) {
            updateWrapper.set(EmployeeSalarySummary::getAccrualVoucherId, voucherChangeDto.getId());
        } else if (voucherType == 1 || voucherType == 3) {
            updateWrapper.set(EmployeeSalarySummary::getSalaryVoucherId, voucherChangeDto.getId());
        }
        updateWrapper.eq(EmployeeSalarySummary::getId, dto.getId());
        super.update(updateWrapper);

        return Message.ok(voucherChangeDto.getId());
    }


    public String generateVoucherItemSummary(String summary, int year, int month) {
        return summary.replace("{yy}", (year + "").substring(2)).replace("{yyyy}", year + "").replace("{mm}", month + "");
    }

    /**
     * Gets the current date formatted as yyyy-MM-dd with time set to 00:00:00
     */
    private Date getFormattedCurrentDate() {
        try {
            Date currentDate = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formattedDateStr = dateFormat.format(currentDate);
            return dateFormat.parse(formattedDateStr);
        } catch (ParseException e) {
            log.error("Error formatting date");
            return new Date();
        }
    }

    /**
     * Creates the voucher change dto with common fields
     */
    private VoucherChangeDto createVoucherChangeDto(Book book, String bookId,
                                                    Date voucherDate, Integer year, Integer month, BigDecimal amount) {

        Integer wordNum = voucherService.getAbleWordNum(bookId, "记", null, null).getData();

        VoucherChangeDto dto = new VoucherChangeDto();
        dto.setWordHead("记");
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
            EmployeeSalaryVoucherRule rule, boolean isDebit, BigDecimal amount) {

        LambdaQueryWrapper<BookSubject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookSubject::getBookId, bookId);
        wrapper.eq(BookSubject::getCode, rule.getSubjectCode());
        BookSubject bookSubject = bookSubjectMapper.selectOne(wrapper);

        VoucherItemChangeDto itemDto = new VoucherItemChangeDto();
        itemDto.setSummary(rule.getSummary());
        itemDto.setSubjectId(bookSubject.getId());
        if (isDebit) {
            itemDto.setDebitAmount(amount);
        } else {
            itemDto.setCreditAmount(amount);
        }
        itemDto.setAuxiliary(List.of());
        itemDto.setSubjectCode(bookSubject.getCode());
        itemDto.setSubjectName(bookSubject.getCode() + "-" + bookSubject.getName());
        itemDto.setDetailedAccounts("");
//        itemDto.setSubjectBalance(BigDecimal.ZERO);

        return itemDto;
    }

    @Override
    public EmployeeSalarySummary selectSalarySummary(SalarySummaryChangeDto dto) {
        return this.baseMapper.selectSalarySummary(dto);
    }

    private List<EmployeeSalaryVoucherRule> getSalaryVoucherRules(String bookId, Integer voucherType) {
        List<EmployeeSalaryVoucherRuleTemplate> employeeSalaryVoucherRuleTemplates = employeeSalaryVoucherRuleTemplateMapper.selectList(Wrappers.<EmployeeSalaryVoucherRuleTemplate>lambdaQuery()
                .eq(EmployeeSalaryVoucherRuleTemplate::getBookId, bookId)
                .eq(EmployeeSalaryVoucherRuleTemplate::getStatus, 1)
                .eq(EmployeeSalaryVoucherRuleTemplate::getVoucherType, voucherType));
        if (ObjectUtils.isEmpty(employeeSalaryVoucherRuleTemplates)) {
            if (Objects.equals(voucherType, 0)) {
                throw new BusinessException(50001, "缺少计提工资凭证模板，请先在工资凭证规则处生成");
            } else {
                throw new BusinessException(50001, "缺少发放工资凭证模板，请先在工资凭证规则处生成");
            }
        }

        String id = employeeSalaryVoucherRuleTemplates.get(0).getId();
        List<EmployeeSalaryVoucherRule> employeeSalaryVoucherRules = employeeSalaryVoucherRuleMapper.selectList(Wrappers.<EmployeeSalaryVoucherRule>lambdaQuery()
                .eq(EmployeeSalaryVoucherRule::getTemplateId, id));

        if (employeeSalaryVoucherRules.size() < 2) {
            throw new BusinessException(50001, "请添加模板明细数据");
        }

        return employeeSalaryVoucherRules;
    }

}
