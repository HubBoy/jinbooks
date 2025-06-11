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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.book.Book;
import com.jinbooks.entity.book.dto.BookChangeDto;
import com.jinbooks.entity.book.dto.BookPageDto;
import com.jinbooks.entity.book.vo.BookVo;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.hr.EmployeeSalarySummary;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRule;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRuleTemplate;
import com.jinbooks.enums.BookBusinessExceptionEnum;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.persistence.mapper.BookMapper;
import com.jinbooks.persistence.service.*;

import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2024/12/31 11:15
 */

@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {
    @Autowired
    IdentifierGenerator identifierGenerator;

    @Autowired
    BookMapper bookMapper;

    @Autowired
    BookSubjectService bookSubjectService;

    @Autowired
    ConfigCashFlowBalanceService configCashFlowBalanceService;

    @Autowired
    StatementIncomeService statementIncomeService;

    @Autowired
    StatementBalanceSheetService statementBalanceSheetService;

    @Autowired
    ConfigSysService configSysService;

    @Autowired
    VoucherService voucherService;

    @Autowired
    VoucherTemplateService voucherTemplateService;

    @Autowired
    StandardSubjectCashFlowService standardSubjectCashFlowService;

    @Autowired
    EmployeeSalaryVoucherRuleTemplateService employeeSalaryVoucherRuleTemplateService;

    @Autowired
    EmployeeSalaryVoucherRuleService employeeSalaryVoucherRuleService;

    @Override
    public Message<Page<Book>> pageList(BookPageDto dto) {
        Page<Book> page = bookMapper.pageList(dto.build(), dto);

        return new Message<>(Message.SUCCESS, page);
    }

    @Override
    @Transactional
    public Message<String> save(BookChangeDto dto) {

        //校验账套名称是否重复
        checkIfTheNameExists(dto, false);

        dto.setId(identifierGenerator.nextId(dto).toString());

        // 账套配置参数初始化
        configSysService.initBooksConfig(dto.getId(),dto.getEnableDate().toString());

        //账套科目
        bookSubjectService.initBookSubject(dto);

        //新增现金流量余额配置
        configCashFlowBalanceService.configCashFlowBalance(dto);

        //新增账套利润表配置
        statementIncomeService.initIncomeStatement(dto);

        //新增账套资产负债表配置
        statementBalanceSheetService.initBalanceSheet(dto);

        //新增默认科目和现金流量的关系
        standardSubjectCashFlowService.saveTemplateRelationships(dto.getId());

        //新增默认工资凭证规则模板
        setSalaryVoucherRule(dto.getId());

        //新增账套
        Book newBook = new Book();
        BeanUtil.copyProperties(dto, newBook);
        boolean saveResult = super.save(newBook);

        return saveResult ? new Message<>(Message.SUCCESS, "新增成功") : new Message<>(Message.FAIL, "新增失败");
    }

    @Override
    @Transactional
    public Message<String> update(BookChangeDto dto) {
        checkIfTheNameExists(dto, true);

        //新增现金流量余额配置
        configCashFlowBalanceService.configCashFlowBalance(dto);

        //更新账套
        Book booksUpdate = new Book();
        BeanUtil.copyProperties(dto, booksUpdate);
        boolean result = super.updateById(booksUpdate);
        return result ? new Message<>(Message.SUCCESS, "修改成功") : new Message<>(Message.FAIL, "修改失败");
    }

    private void checkIfTheNameExists(BookChangeDto dto, boolean isEdit) {
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getName, dto.getName());
        if (isEdit) {
            wrapper.ne(Book::getId, dto.getId());
        }
        List<Book> list = super.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(
                    BookBusinessExceptionEnum.DUPLICATE_SETNAME_EXIST.getCode(),
                    BookBusinessExceptionEnum.DUPLICATE_SETNAME_EXIST.getMsg()
            );
        }
    }


    @Override
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> bookIds = dto.getListIds();

        //校验是否为活跃状态
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, 1);
        wrapper.in(Book::getId, bookIds);
        List<Book> books = bookMapper.selectList(wrapper);
        if (ObjectUtils.isNotEmpty(books)) {
            throw new BusinessException(
                    BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE.getCode(),
                    BookBusinessExceptionEnum.DISABLE_BEFORE_DELETE.getMsg()
            );
        }

        //删除关联科目
        bookSubjectService.deleteByBookIds(bookIds);

        //删除现金流量余额配置
        configCashFlowBalanceService.deleteByBookIds(bookIds);

        //删除现金流量和科目的默认关系
        standardSubjectCashFlowService.deleteByBookIds(bookIds);

        //删除利润表配置及数据
    	statementIncomeService.deleteByBookIds(bookIds);

    	//删除资产负债表配置和数据
    	statementBalanceSheetService.deleteByBookIds(bookIds);

    	//删除科目余额表数据
    	statementBalanceSheetService.deleteByBookIds(bookIds);

    	//删除凭证模板及相关条目配置
    	voucherTemplateService.deleteByBookIds(bookIds);

        //删除凭证及相关条目
    	voucherService.deleteByBookIds(bookIds);

        //删除账套数据
        boolean result = super.removeByIds(bookIds);

        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    @Override
    public List<BookVo> listBooks(String userId) {
        return bookMapper.listBooks(userId);
    }

    private void setSalaryVoucherRule(String bookId) {
        // 1. 查询内置的模板数据（bookId为null的数据）
        List<EmployeeSalaryVoucherRuleTemplate> builtInTemplates = employeeSalaryVoucherRuleTemplateService.list(
                Wrappers.<EmployeeSalaryVoucherRuleTemplate>lambdaQuery()
                        .isNull(EmployeeSalaryVoucherRuleTemplate::getBookId)
        );

        if (ObjectUtils.isEmpty(builtInTemplates)) {
            return; // 如果没有内置模板数据，直接返回
        }

        // 2. 获取内置模板的ID列表
        List<String> builtInTemplateIds = builtInTemplates.stream()
                .map(EmployeeSalaryVoucherRuleTemplate::getId)
                .toList();

        // 3. 查询对应的内置规则数据
        List<EmployeeSalaryVoucherRule> builtInRules = employeeSalaryVoucherRuleService.list(
                Wrappers.<EmployeeSalaryVoucherRule>lambdaQuery()
                        .in(EmployeeSalaryVoucherRule::getTemplateId, builtInTemplateIds)
        );

        // 4. 复制模板数据并设置新的bookId，同时建立映射关系
        CopyOptions copyOptions = new CopyOptions();
        copyOptions.setIgnoreProperties("id", "createdBy", "createdDate", "modifiedBy", "modifiedDate");

        Map<String, String> templateIdMapping = new HashMap<>();

        // 逐个处理模板数据，确保映射关系正确
        for (EmployeeSalaryVoucherRuleTemplate builtInTemplate : builtInTemplates) {
            String oldTemplateId = builtInTemplate.getId();

            // 复制模板数据
            EmployeeSalaryVoucherRuleTemplate newTemplate = BeanUtil.copyProperties(
                    builtInTemplate,
                    EmployeeSalaryVoucherRuleTemplate.class,
                    "id", "createdBy", "createdDate", "modifiedBy", "modifiedDate", "deleted"
            );
            newTemplate.setBookId(bookId);

            // 保存单个模板以获取新生成的ID
            employeeSalaryVoucherRuleTemplateService.save(newTemplate);

            // 建立新旧ID映射关系
            String newTemplateId = newTemplate.getId();
            templateIdMapping.put(oldTemplateId, newTemplateId);
        }

        // 5. 复制规则数据并设置新的templateId
        if (ObjectUtils.isNotEmpty(builtInRules)) {
            List<EmployeeSalaryVoucherRule> newRules = BeanUtil.copyToList(
                    builtInRules,
                    EmployeeSalaryVoucherRule.class,
                    copyOptions
            );

            // 更新规则数据的templateId为新模板的ID
            newRules.forEach(rule -> {
                String oldTemplateId = rule.getTemplateId();
                String newTemplateId = templateIdMapping.get(oldTemplateId);
                rule.setTemplateId(newTemplateId);
            });

            // 6. 批量保存新的规则数据
            employeeSalaryVoucherRuleService.saveBatch(newRules);
        }
    }

}
