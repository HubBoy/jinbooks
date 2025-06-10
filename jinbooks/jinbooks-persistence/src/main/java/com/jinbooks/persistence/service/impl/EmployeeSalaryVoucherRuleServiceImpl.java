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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.dto.ChangeStatusDto;
import com.jinbooks.entity.dto.ListIdsDto;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRule;
import com.jinbooks.entity.hr.EmployeeSalaryVoucherRuleTemplate;
import com.jinbooks.entity.hr.dto.EmployeeSalaryVoucherRulePageDto;
import com.jinbooks.entity.hr.dto.SalaryTemplateDetailDto;
import com.jinbooks.entity.hr.dto.SalaryTemplateDto;
import com.jinbooks.entity.hr.vo.SalaryTemplateVo;
import com.jinbooks.exception.BusinessException;
import com.jinbooks.persistence.mapper.EmployeeSalaryVoucherRuleMapper;
import com.jinbooks.persistence.service.EmployeeSalaryVoucherRuleService;
import com.jinbooks.persistence.service.EmployeeSalaryVoucherRuleTemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: orangeBabu
 * @time: 2025/3/11 10:47
 */

@Service
@RequiredArgsConstructor
public class EmployeeSalaryVoucherRuleServiceImpl extends ServiceImpl<EmployeeSalaryVoucherRuleMapper, EmployeeSalaryVoucherRule> implements EmployeeSalaryVoucherRuleService {

    private final EmployeeSalaryVoucherRuleTemplateService employeeSalaryVoucherRuleTemplateService;

    @Override
    public Message<Page<EmployeeSalaryVoucherRuleTemplate>> pageList(EmployeeSalaryVoucherRulePageDto dto) {

        LambdaQueryWrapper<EmployeeSalaryVoucherRuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeSalaryVoucherRuleTemplate::getBookId, dto.getBookId());

        Page<EmployeeSalaryVoucherRuleTemplate> page = employeeSalaryVoucherRuleTemplateService.page(dto.build(), wrapper);

        return Message.ok(page);
    }

    @Override
    @Transactional
    public Message<String> save(SalaryTemplateDto dto) {

        checkTemplate(dto, false);

        //凭证类型
        Integer voucherType = dto.getVoucherType();
        //凭证字
        String wordHead = dto.getWordHead();
        //账套ID
        String bookId = dto.getBookId();

        EmployeeSalaryVoucherRuleTemplate employeeSalaryVoucherRuleTemplate = new EmployeeSalaryVoucherRuleTemplate();
        employeeSalaryVoucherRuleTemplate.setVoucherType(voucherType);
        employeeSalaryVoucherRuleTemplate.setWordHead(wordHead);
        employeeSalaryVoucherRuleTemplate.setBookId(bookId);
        boolean templateResult = employeeSalaryVoucherRuleTemplateService.save(employeeSalaryVoucherRuleTemplate);

        if (!templateResult) {
            return Message.failed("新增模板失败");
        }

        List<SalaryTemplateDetailDto> salaryTemplateDetailDtos = dto.getSalaryTemplateDetailDtos();

        List<EmployeeSalaryVoucherRule> employeeSalaryVoucherRules = new ArrayList<>();

        for (SalaryTemplateDetailDto salaryTemplateDetailDto : salaryTemplateDetailDtos) {
            EmployeeSalaryVoucherRule employeeSalaryVoucherRule = BeanUtil.copyProperties(salaryTemplateDetailDto, EmployeeSalaryVoucherRule.class);
            employeeSalaryVoucherRule.setTemplateId(employeeSalaryVoucherRuleTemplate.getId());
            employeeSalaryVoucherRules.add(employeeSalaryVoucherRule);
        }

        boolean save = super.saveBatch(employeeSalaryVoucherRules);

        return save ? Message.ok("新增成功") : Message.failed("新增失败");
    }

    @Override
    @Transactional
    public Message<String> update(SalaryTemplateDto dto) {
        checkTemplate(dto, true);

        //找出已经存在的数据
        List<EmployeeSalaryVoucherRule> employeeSalaryVoucherRules = super.list(Wrappers.<EmployeeSalaryVoucherRule>lambdaQuery()
                .eq(EmployeeSalaryVoucherRule::getTemplateId, dto.getId()));
        //入参集合
        List<SalaryTemplateDetailDto> salaryTemplateDetailDtos = dto.getSalaryTemplateDetailDtos();

        // 3.1 找出要新增的记录（入参ID为null）
        List<SalaryTemplateDetailDto> toAddList = salaryTemplateDetailDtos.stream()
                .filter(item -> StringUtils.isEmpty(item.getId()))
                .toList();

        // 3.2 创建现有记录的ID映射，用于快速查找
        Map<String, EmployeeSalaryVoucherRule> existingMap = employeeSalaryVoucherRules.stream()
                .collect(Collectors.toMap(EmployeeSalaryVoucherRule::getId, Function.identity()));

        // 3.3 创建入参记录的ID映射，用于快速查找
        Map<String, SalaryTemplateDetailDto> inputMap = salaryTemplateDetailDtos.stream()
                .filter(item -> StringUtils.isNotEmpty(item.getId()))
                .collect(Collectors.toMap(SalaryTemplateDetailDto::getId, Function.identity()));

        // 3.4 找出要删除的记录（在现有记录中但不在入参中）
        List<EmployeeSalaryVoucherRule> toDeleteList = employeeSalaryVoucherRules.stream()
                .filter(item -> !inputMap.containsKey(item.getId()))
                .toList();

        // 3.5 找出要更新的记录（两边都有）
        List<SalaryTemplateDetailDto> toUpdateList = salaryTemplateDetailDtos.stream()
                .filter(item -> StringUtils.isNotEmpty(item.getId()) && existingMap.containsKey(item.getId()))
                .toList();

        // 4. 处理新增
        if (!toAddList.isEmpty()) {
            List<EmployeeSalaryVoucherRule> newRules = toAddList.stream()
                    .map(this::convertToRule)  // 需要实现一个转换方法
                    .peek(rule -> rule.setTemplateId(dto.getId()))
                    .toList();

            super.saveBatch(newRules);
        }

        // 5. 处理删除
        if (!toDeleteList.isEmpty()) {
            List<String> idsToDelete = toDeleteList.stream()
                    .map(EmployeeSalaryVoucherRule::getId)
                    .toList();

            super.removeByIds(idsToDelete);
        }

        // 6. 处理更新
        if (!toUpdateList.isEmpty()) {
            List<EmployeeSalaryVoucherRule> updatedRules = toUpdateList.stream()
                    .map(item -> {
                        EmployeeSalaryVoucherRule existingRule = existingMap.get(item.getId());
                        // 更新规则的属性
                        updateRuleFromDto(existingRule, item);
                        return existingRule;
                    })
                    .toList();

            super.updateBatchById(updatedRules);
        }


      /*  BookSubject bookSubject = bookSubjectMapper.selectById(dto.getSubjectId());
        String auxiliary = bookSubject.getAuxiliary();
        if (StringUtils.isNotBlank(auxiliary) && !"[]".equals(auxiliary)) {
            throw new BusinessException(500001, "不能选择辅助核算科目作为工资凭证规则");
        }

        EmployeeSalaryVoucherRule employeeSalaryVoucherRule = BeanUtil.copyProperties(dto, EmployeeSalaryVoucherRule.class);
        boolean result = super.updateById(employeeSalaryVoucherRule);
        return result ? Message.ok("修改成功") : Message.failed("修改失败");*/
        return Message.ok("修改成功");
    }

    @Override
    @Transactional
    public Message<String> delete(ListIdsDto dto) {
        List<String> ids = dto.getListIds();
        //状态检查
        List<EmployeeSalaryVoucherRuleTemplate> list = employeeSalaryVoucherRuleTemplateService.list(Wrappers.<EmployeeSalaryVoucherRuleTemplate>lambdaQuery()
                .eq(EmployeeSalaryVoucherRuleTemplate::getStatus, 1)
                .in(EmployeeSalaryVoucherRuleTemplate::getId, ids));
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(50001, "请先禁用当前数据再进行删除操作");
        }
        //删除模板数据
        boolean result = employeeSalaryVoucherRuleTemplateService.removeBatchByIds(ids);
        //删除明细数据
        super.remove(Wrappers.<EmployeeSalaryVoucherRule>lambdaQuery()
                .in(EmployeeSalaryVoucherRule::getTemplateId, ids));
        return result ? new Message<>(Message.SUCCESS, "删除成功") : new Message<>(Message.FAIL, "删除失败");
    }

    @Override
    public Message<String> changeStatus(ChangeStatusDto dto) {
        Integer status = dto.getStatus();
        String id = dto.getId();

        EmployeeSalaryVoucherRuleTemplate employeeSalaryVoucherRuleTemplate = employeeSalaryVoucherRuleTemplateService.getById(id);

        //启用
        if (Objects.equals(status, 1)) {
            SalaryTemplateDto salaryTemplateDto = new SalaryTemplateDto();
            salaryTemplateDto.setBookId(employeeSalaryVoucherRuleTemplate.getBookId());
            salaryTemplateDto.setVoucherType(employeeSalaryVoucherRuleTemplate.getVoucherType());
            checkTemplate(salaryTemplateDto, false);
        }

        employeeSalaryVoucherRuleTemplate.setStatus(status);

        boolean result = employeeSalaryVoucherRuleTemplateService.updateById(employeeSalaryVoucherRuleTemplate);

        return result ? Message.ok("操作成功") : Message.failed("操作失败");
    }

    @Override
    public Message<SalaryTemplateVo> getTemplateDetail(String id) {
        EmployeeSalaryVoucherRuleTemplate template = employeeSalaryVoucherRuleTemplateService.getById(id);
        SalaryTemplateVo salaryTemplateVo = BeanUtil.copyProperties(template, SalaryTemplateVo.class);

        List<EmployeeSalaryVoucherRule> list = super.list(Wrappers.<EmployeeSalaryVoucherRule>lambdaQuery()
                .eq(EmployeeSalaryVoucherRule::getTemplateId, id));

        salaryTemplateVo.setEmployeeSalaryVoucherRules(list);

        return Message.ok(salaryTemplateVo);
    }

    private void checkTemplate(SalaryTemplateDto dto, boolean isEdit) {
        LambdaQueryWrapper<EmployeeSalaryVoucherRuleTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeSalaryVoucherRuleTemplate::getBookId, dto.getBookId());
        wrapper.eq(EmployeeSalaryVoucherRuleTemplate::getStatus, 1);
        wrapper.eq(EmployeeSalaryVoucherRuleTemplate::getVoucherType, dto.getVoucherType());
        if (isEdit) {
            wrapper.ne(EmployeeSalaryVoucherRuleTemplate::getId, dto.getId());
        }

        List<EmployeeSalaryVoucherRuleTemplate> list = employeeSalaryVoucherRuleTemplateService.list(wrapper);
        if (ObjectUtils.isNotEmpty(list)) {
            throw new BusinessException(50001, "保存失败！该薪资类型的模版已被启用");
        }
    }

    // 需要实现的辅助方法
    private EmployeeSalaryVoucherRule convertToRule(SalaryTemplateDetailDto dto) {
        EmployeeSalaryVoucherRule rule = new EmployeeSalaryVoucherRule();
        // 设置属性
        rule.setSummary(dto.getSummary());
        rule.setDirection(dto.getDirection());
        rule.setSubjectCode(dto.getSubjectCode());
        rule.setSelectedValue(dto.getSelectedValue());
        // 设置其他属性...
        return rule;
    }

    private void updateRuleFromDto(EmployeeSalaryVoucherRule rule, SalaryTemplateDetailDto dto) {
        // 更新属性
        rule.setSummary(dto.getSummary());
        rule.setDirection(dto.getDirection());
        rule.setSubjectCode(dto.getSubjectCode());
        rule.setSelectedValue(dto.getSelectedValue());
        // 更新其他属性...
    }
}
