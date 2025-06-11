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
import com.jinbooks.constants.ConstsSysConfig;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.book.Book;
import com.jinbooks.entity.book.Settlement;
import com.jinbooks.entity.book.dto.BookChangeDto;
import com.jinbooks.entity.standard.StandardStatementBalanceSheet;
import com.jinbooks.entity.standard.StandardStatementRules;
import com.jinbooks.entity.statement.*;
import com.jinbooks.entity.statement.dto.StatementParamsDto;
import com.jinbooks.entity.statement.vo.StatementBalanceSheetExport;
import com.jinbooks.entity.statement.vo.StatementBalanceSheetItemListVo;
import com.jinbooks.enums.StatementPeriodTypeEnum;
import com.jinbooks.enums.StatementSymbolEnum;
import com.jinbooks.enums.StatementTypeEnum;
import com.jinbooks.persistence.mapper.*;
import com.jinbooks.persistence.service.ConfigSysService;
import com.jinbooks.persistence.service.StatementBalanceSheetConfigService;
import com.jinbooks.persistence.service.StatementBalanceSheetService;
import com.jinbooks.util.excel.ExcelDataModeEnum;
import com.jinbooks.util.excel.ExcelExporter;
import com.jinbooks.util.excel.ExcelParams;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RequiredArgsConstructor
@Service
public class StatementBalanceSheetServiceImpl implements StatementBalanceSheetService {
    private final ConfigSysService configSysService;
    private final StatementBalanceSheetMapper balanceSheetMapper;
    private final StatementBalanceSheetItemMapper balanceSheetItemMapper;
    private final StatementBalanceSheetConfigService balanceSheetConfigService;
    private final IdentifierGenerator identifierGenerator;
    private final StatementRulesMapper statementRulesMapper;
    private final StandardStatementBalanceSheetMapper standardStatementBalanceSheetMapper;
    private final StandardStatementRulesMapper standardStatementRulesMapper;
    private final BookMapper bookMapper;

    /**
     * 报表-资产负债表
     *
     * @param dto   查询参数
     * @param force 是否强制实时统计,如果为否，则非当前期从报表中查询，否则从科目余额表查询
     * @return 结果
     */
    @Override
    @Transactional
    public Message<StatementBalanceSheet> queryBalanceSheet(StatementParamsDto dto, boolean force) {
        dto.parse();
        // 查询历史报表
        LambdaQueryWrapper<StatementBalanceSheet> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementBalanceSheet::getBookId, dto.getBookId());
        lqw.eq(StatementBalanceSheet::getYearPeriod, dto.getReportDate());
        StatementBalanceSheet balanceSheet = balanceSheetMapper.selectOne(lqw);

        String currentTerm = configSysService.getCurrentTerm(dto.getBookId());
        List<String> allMonths = dto.getAllMonths();
        List<StatementBalanceSheetItem> items;
        // 查询范围包含了当前期,或者没数据，则实时统计
        boolean isNull = balanceSheet == null;
        if (isNull || allMonths.contains(currentTerm) || force) {
            balanceSheet = new StatementBalanceSheet();
            String currentId = identifierGenerator.nextId(balanceSheet).toString();
            balanceSheet.setId(currentId);
            balanceSheet.setBookId(dto.getBookId());
            balanceSheet.setYearPeriod(dto.getReportDate());
            balanceSheet.setPeriodType(dto.getPeriodType());

            // 查询条目
            LambdaQueryWrapper<StatementBalanceSheetItem> itemLqw = Wrappers.lambdaQuery();
            itemLqw.eq(StatementBalanceSheetItem::getBookId, dto.getBookId());
            itemLqw.eq(StatementBalanceSheetItem::getBalanceSheetId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
            items = balanceSheetItemMapper.selectList(itemLqw);

            if (!items.isEmpty()) {
                // 插入当前数据
                if (isNull
                        && !StatementPeriodTypeEnum.BETWEEN_MONTH.getValue().equals(dto.getPeriodType())) {
                    for (StatementBalanceSheetItem item : items) {
                        String itemId = identifierGenerator.nextId(item).toString();
                        item.setId(itemId);
                        item.setBalanceSheetId(currentId);
                        item.setBookId(dto.getBookId());
                        item.setInitialBalance(BigDecimal.ZERO);
                        item.setCurrentBalance(BigDecimal.ZERO);
                        balanceSheetItemMapper.insert(item);
                    }

                    balanceSheetMapper.insert(balanceSheet);
                }

                // 遍历月份，统计总金额
                for (String month : allMonths) {
                    balanceSheetConfigService.refreshItemsBalance(items, dto.getBookId(), month);
                }
            }
        }else {// 拉取历史数据
            // 查询条目
            LambdaQueryWrapper<StatementBalanceSheetItem> itemLqw = Wrappers.lambdaQuery();
            itemLqw.eq(StatementBalanceSheetItem::getBookId, dto.getBookId());
            itemLqw.eq(StatementBalanceSheetItem::getBalanceSheetId, balanceSheet.getId());
            items = balanceSheetItemMapper.selectList(itemLqw);
        }

        StatementBalanceSheetItemListVo itemListVo = balanceSheetConfigService.insertSubtotals(items);
        itemListVo.getAssets().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        itemListVo.getLiability().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        balanceSheet.setItems(itemListVo);
        return new Message<>(balanceSheet);
    }

    /**
     * 更新资产负载表。
     *
     * @param dto  查询参数
     * @param save 是否更新入库，为true时会将当前查询到的数据更新入库。原有数据会覆盖。
     * @return 统计结果
     */
    @Override
    @Transactional
    public Message<StatementBalanceSheet> balanceSheet(StatementParamsDto dto, boolean save) {
        dto.parse();
        StatementBalanceSheet statementBalanceSheet = queryBalanceSheet(dto, true).getData();

        if (save) {
            StatementBalanceSheetItemListVo itemListVo = statementBalanceSheet.getItems();
            List<StatementBalanceSheetItem> balanceSheetItems = itemListVo.getAssets();
            balanceSheetItems.addAll(itemListVo.getLiability());
            LambdaQueryWrapper<StatementBalanceSheetItem> itemLqw = Wrappers.lambdaQuery();
            itemLqw.eq(StatementBalanceSheetItem::getBookId, dto.getBookId());
            itemLqw.eq(StatementBalanceSheetItem::getBalanceSheetId, statementBalanceSheet.getId());
            balanceSheetItemMapper.delete(itemLqw);

            for (StatementBalanceSheetItem item : balanceSheetItems) {
                String itemId = identifierGenerator.nextId(item).toString();
                item.setId(itemId);
                item.setBalanceSheetId(statementBalanceSheet.getId());
                item.setBookId(dto.getBookId());
            }
            balanceSheetItemMapper.insertBatch(balanceSheetItems);
        }
        return Message.ok(statementBalanceSheet);
    }

    @Override
    public void initBalanceSheet(BookChangeDto dto) {
        LambdaQueryWrapper<StandardStatementBalanceSheet> sItemlqw = Wrappers.lambdaQuery();
        sItemlqw.eq(StandardStatementBalanceSheet::getStandardId, dto.getStandardId());
        List<StandardStatementBalanceSheet> items = standardStatementBalanceSheetMapper.selectList(sItemlqw);
        List<StatementBalanceSheetItem> balanceSheetItems = new ArrayList<>();
        for (StandardStatementBalanceSheet item : items) {
            StatementBalanceSheetItem balanceSheetItem = new StatementBalanceSheetItem();
            balanceSheetItem.setBookId(dto.getId());
            balanceSheetItem.setBalanceSheetId(ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);

            balanceSheetItem.setAssetOrLiability(item.getAssetOrLiability());
            balanceSheetItem.setItemCode(item.getItemCode());
            balanceSheetItem.setItemName(item.getItemName());
            balanceSheetItem.setSortIndex(item.getSortIndex());
            balanceSheetItem.setLevel(item.getLevel());
            balanceSheetItem.setParentItemCode(item.getParentItemCode());
            balanceSheetItem.setSymbol(item.getSymbol());
            balanceSheetItem.setRule(item.getRule());
            balanceSheetItem.setCurrentBalance(BigDecimal.ZERO);
            balanceSheetItem.setInitialBalance(BigDecimal.ZERO);

            balanceSheetItems.add(balanceSheetItem);
        }

        LambdaQueryWrapper<StandardStatementRules> sRulelqw = Wrappers.lambdaQuery();
        sRulelqw.eq(StandardStatementRules::getStandardId, dto.getStandardId());
        sRulelqw.eq(StandardStatementRules::getType, StatementTypeEnum.balance_sheet.name());
        List<StandardStatementRules> sRuleitems = standardStatementRulesMapper.selectList(sRulelqw);

        List<StatementRules> itemRuls = new ArrayList<>();
        for (StandardStatementRules itemRule : sRuleitems) {
            StatementRules rule = new StatementRules();
            rule.setBookId(dto.getId());
            rule.setType(StatementTypeEnum.balance_sheet.name());
            rule.setType(itemRule.getType());
            rule.setItemCode(itemRule.getItemCode());
            rule.setSubjectCode(itemRule.getSubjectCode());
            rule.setRule(itemRule.getRule());
            rule.setSymbol(itemRule.getSymbol());
            itemRuls.add(rule);
        }

        balanceSheetItemMapper.insertBatch(balanceSheetItems);
        statementRulesMapper.insertBatch(itemRuls);

    }

    /**
     * 数据导出
     */
    @Override
    @Transactional
    public void export(StatementParamsDto dto, HttpServletResponse response) throws IOException {
        StatementBalanceSheet balanceSheet = queryBalanceSheet(dto, true).getData();
        StatementBalanceSheetItemListVo itemListVo = balanceSheet.getItems();
        List<StatementBalanceSheetItem> assets = itemListVo.getAssets();
        List<StatementBalanceSheetItem> liability = itemListVo.getLiability();
        List<StatementBalanceSheetItem> maxData = assets.size() > liability.size() ? assets : liability;
        Book book = bookMapper.selectById(dto.getBookId());

        String templatePath = ResourceUtils
                .getURL("classpath:static/export-template/template-balance-sheet.xlsx")
                .getPath();
        // 注意：Windows下getPath()前会带'/'，可做处理
        if (templatePath.startsWith("/")) {
            templatePath = templatePath.substring(1);
        }
        Path tempFilePath = Files.createTempFile("template-balance-sheet_", ".xlsx");
        File tempFile = tempFilePath.toFile();

        // 组装数据
        List<StatementBalanceSheetExport.AssetLiability> assetLiabilityList = new ArrayList<>();
        StatementBalanceSheetExport data = StatementBalanceSheetExport.builder()
                .companyName(book.getCompanyName())
                .date(balanceSheet.getYearPeriod())
                .assetLiabilityList(assetLiabilityList)
                .build();
        for (int i = 0; i < maxData.size(); i++) {
            StatementBalanceSheetItem assetItem = i < assets.size() ? assets.get(i) : null;
            StatementBalanceSheetItem liabilityItem = i < liability.size() ? liability.get(i) : null;
            StatementBalanceSheetExport.AssetLiability assetLiability = StatementBalanceSheetExport.AssetLiability.builder()
                    .assetItemName(
                            assetItem != null
                                    ? (StatementSymbolEnum.MINUS.getValue().equals(assetItem.getSymbol()) ? "减：" : "") + assetItem.getItemName()
                                    : null
                    )
                    .assetRowNum(assetItem != null ? assetItem.getSortIndex() : null)
                    .assetCurrentBalance(assetItem != null && assetItem.getCurrentBalance() != null ? assetItem.getCurrentBalance() : null)
                    .assetInitialBalance(assetItem != null && assetItem.getInitialBalance() != null ? assetItem.getInitialBalance() : null)
                    .liabilityItemName(
                            liabilityItem != null
                                    ? (StatementSymbolEnum.MINUS.getValue().equals(liabilityItem.getSymbol()) ? "减：" : "") + liabilityItem.getItemName()
                                    : null
                    )
                    .liabilityRowNum(liabilityItem != null ? liabilityItem.getSortIndex() : null)
                    .liabilityCurrentBalance(liabilityItem != null && liabilityItem.getCurrentBalance() != null ? liabilityItem.getCurrentBalance() : null)
                    .liabilityInitialBalance(liabilityItem != null && liabilityItem.getInitialBalance() != null ? liabilityItem.getInitialBalance() : null)
                    .build();
            assetLiabilityList.add(assetLiability);
        }

        // 单项数据渲染
        ExcelParams<StatementBalanceSheetExport> paramsObj = ExcelParams.<StatementBalanceSheetExport>builder()
                .httpResponse(null)
                .mode(ExcelDataModeEnum.base_attribute)
                .dataModel(data)
                .outputDirectory(tempFile.getParent()) // 临时目录路径
                .outputFileName(tempFile.getName())    // 临时文件名
                .enableMergeCells(false)
                .autoSizeColumns(false)
                .recalculateFormulas(true)
                .templateFilePath(templatePath)
                .build();
        ExcelExporter.export(paramsObj);
        // 列表数据渲染
        ExcelParams<List<StatementBalanceSheetExport.AssetLiability>> paramsList = ExcelParams.<List<StatementBalanceSheetExport.AssetLiability>>builder()
                .httpResponse(response)
                .mode(ExcelDataModeEnum.include_list)
                .dataModel(assetLiabilityList)
                .enableMergeCells(false)
                .autoSizeColumns(false)
                .recalculateFormulas(true)
                .templateFilePath(tempFile.getPath())
                .build();

        ExcelExporter.export(paramsList);
        // 最后删除临时文件
        if (tempFile.exists()) tempFile.delete();
    }

    @Override
    public boolean deleteByBookIds(List<String> bookIds) {
        LambdaQueryWrapper<StatementBalanceSheet> slqw = Wrappers.lambdaQuery();
        slqw.in(StatementBalanceSheet::getBookId, bookIds);
        balanceSheetMapper.delete(slqw);

        LambdaQueryWrapper<StatementBalanceSheetItem> sItemlqw = Wrappers.lambdaQuery();
        sItemlqw.in(StatementBalanceSheetItem::getBookId, bookIds);
        balanceSheetItemMapper.delete(sItemlqw);

        LambdaQueryWrapper<StatementRules> sRulelqw = Wrappers.lambdaQuery();
        sRulelqw.in(StatementRules::getBookId, bookIds);
        sRulelqw.eq(StatementRules::getType, "balance_sheet");
        statementRulesMapper.delete(sRulelqw);
        return true;
    }

    /**
     * 指定期结账入库
     *
     * @param dto 结账参数
     * @return 操作结果
     */
    @Override
    @Transactional
    public boolean checkout(Settlement dto) {
        // 月报
        StatementParamsDto monthParamsDto = new StatementParamsDto();
        monthParamsDto.setBookId(dto.getBookId());
        monthParamsDto.setPeriodType(StatementPeriodTypeEnum.MONTH.getValue());
        monthParamsDto.setReportDate(dto.getCurrentTerm());
        balanceSheet(monthParamsDto, true);

        // 季报
        StatementParamsDto quarterParamsDto = new StatementParamsDto();
        quarterParamsDto.setBookId(dto.getBookId());
        quarterParamsDto.setPeriodType(StatementPeriodTypeEnum.QUARTER.getValue());
        quarterParamsDto.setReportDate(dto.getCurrentTerm());
        if (monthParamsDto.isQuarterReportMonth()) {
            balanceSheet(quarterParamsDto, true);
        }

        // 年报
        StatementParamsDto yearParamsDto = new StatementParamsDto();
        yearParamsDto.setBookId(dto.getBookId());
        yearParamsDto.setPeriodType(StatementPeriodTypeEnum.YEAR.getValue());
        yearParamsDto.setReportDate(dto.getCurrentTerm());
        if (monthParamsDto.isYearReportMonth()) {
            balanceSheet(yearParamsDto, true);
        }

        return true;
    }
}
