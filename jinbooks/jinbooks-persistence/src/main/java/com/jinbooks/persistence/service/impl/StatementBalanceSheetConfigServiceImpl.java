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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jinbooks.constants.ConstsSysConfig;
import com.jinbooks.entity.Message;
import com.jinbooks.entity.statement.StatementBalanceSheet;
import com.jinbooks.entity.statement.StatementBalanceSheetItem;
import com.jinbooks.entity.statement.StatementRules;
import com.jinbooks.entity.statement.StatementSubjectBalance;
import com.jinbooks.entity.statement.vo.StatementBalanceSheetItemListVo;

import java.util.function.Function;

import com.jinbooks.enums.AssetOrLiabilityEnum;
import com.jinbooks.enums.StatementPeriodTypeEnum;
import com.jinbooks.enums.StatementSymbolEnum;
import com.jinbooks.enums.StatementTypeEnum;
import com.jinbooks.persistence.mapper.StatementBalanceSheetItemMapper;
import com.jinbooks.persistence.mapper.StatementBalanceSheetMapper;
import com.jinbooks.persistence.mapper.StatementRulesMapper;
import com.jinbooks.persistence.mapper.StatementSubjectBalanceMapper;
import com.jinbooks.persistence.service.ConfigSysService;
import com.jinbooks.persistence.service.StatementBalanceSheetConfigService;
import lombok.RequiredArgsConstructor;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class StatementBalanceSheetConfigServiceImpl implements StatementBalanceSheetConfigService {
    private final StatementBalanceSheetMapper balanceSheetMapper;
    private final StatementBalanceSheetItemMapper statementBalanceSheetItemMapper;
    private final StatementRulesMapper rulesMapper;
    private final StatementSubjectBalanceMapper subjectBalanceMapper;
    private final ConfigSysService configSysService;

    /**
     * 获取资产负载配置明细
     *
     * @return 结果
     */
    @Override
    public Message<StatementBalanceSheetItem> get(String bookId, String itemCode) {
        LambdaQueryWrapper<StatementBalanceSheetItem> itemLqw = Wrappers.lambdaQuery();
        itemLqw.eq(StatementBalanceSheetItem::getBookId, bookId);
        itemLqw.eq(StatementBalanceSheetItem::getBalanceSheetId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
        itemLqw.eq(StatementBalanceSheetItem::getItemCode, itemCode);
        StatementBalanceSheetItem balanceSheetItem = statementBalanceSheetItemMapper.selectOne(itemLqw);

        LambdaQueryWrapper<StatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementRules::getBookId, bookId);
        lqw.eq(StatementRules::getItemCode, balanceSheetItem.getItemCode());
        lqw.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        List<StatementRules> rules = rulesMapper.selectList(lqw);

        if (!rules.isEmpty()) {
            Map<String, StatementRules> rulesMap = new HashMap<>();
            List<String> codes = rules.stream().map(item -> {
                rulesMap.put(item.getSubjectCode(), item);
                return item.getSubjectCode();
            }).toList();
            LambdaQueryWrapper<StatementSubjectBalance> lqwSubject = Wrappers.lambdaQuery();
            lqwSubject.eq(StatementSubjectBalance::getBookId, bookId);
            lqwSubject.in(StatementSubjectBalance::getSubjectCode, codes);
            List<StatementSubjectBalance> subjectBalances = subjectBalanceMapper.selectList(lqwSubject);
            for (StatementSubjectBalance subjectBalance : subjectBalances) {
                StatementRules statementRules = rulesMap.get(subjectBalance.getSubjectCode());
                updateRuleBalance(subjectBalance, statementRules);
            }
        }
        balanceSheetItem.setRules(rules);
        return Message.ok(balanceSheetItem);
    }

    /**
     * 获取资产负载表配置
     *
     * @param bookId 账簿ID
     * @return 结果
     */
    @Override
    public Message<StatementBalanceSheetItemListVo> list(String bookId) {
        //StatementBalanceSheet balanceSheet = getBalanceSheetCurrentPeriod(bookId);
        LambdaQueryWrapper<StatementBalanceSheetItem> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementBalanceSheetItem::getBookId, bookId);
        lqw.eq(StatementBalanceSheetItem::getBalanceSheetId, ConstsSysConfig.SYS_CONFIG_TEMPLATE_ID);
        List<StatementBalanceSheetItem> balanceSheets = statementBalanceSheetItemMapper.selectList(lqw);
        refreshItemsBalance(balanceSheets, bookId, configSysService.getCurrentTerm(bookId));
        StatementBalanceSheetItemListVo itemListVo = insertSubtotals(balanceSheets);
        itemListVo.getAssets().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        itemListVo.getLiability().sort(Comparator.comparing(StatementBalanceSheetItem::getItemCode));
        return Message.ok(itemListVo);
    }

    /**
     * 资产负载表配置
     *
     * @param dto 配置参数
     * @return 结果
     */
    @Override
    @Transactional
    public Message<StatementBalanceSheetItem> save(StatementBalanceSheetItem dto) {
        StatementBalanceSheetItem balanceSheetItem = statementBalanceSheetItemMapper.selectById(dto.getId());
        if (balanceSheetItem == null) {
            return Message.failed("操作不被允许（配置项不存在）");
        }
//        updateSortIndex(dto, StatementSymbolEnum.PLUS);
        statementBalanceSheetItemMapper.updateById(dto);
        // 规则更新
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            for (StatementRules rule : dto.getRules()) {
                rule.setItemCode(dto.getItemCode());
                rule.setBookId(dto.getBookId());
                rule.setType(StatementTypeEnum.balance_sheet.name());
            }
            saveRules(dto.getRules(), dto.getBookId(), dto.getItemCode());
        }

        return Message.ok(dto);
    }

    /**
     * 报表规则配置
     *
     * @param dto 配置项
     * @return 结果
     */
    @Override
    public Message<List<StatementRules>> saveRules(List<StatementRules> dto, String bookId, String code) {
        LambdaQueryWrapper<StatementRules> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StatementRules::getItemCode, code);
        queryWrapper.eq(StatementRules::getBookId, bookId);
        queryWrapper.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        rulesMapper.delete(queryWrapper);
        if (!dto.isEmpty()) {
            rulesMapper.insertOrUpdateBatch(dto);
        }
        return Message.ok(dto);
    }

    @Override
    public Message<Boolean> delete(String id) {
        StatementBalanceSheetItem balanceSheet = statementBalanceSheetItemMapper.selectById(id);
        updateSortIndex(balanceSheet, StatementSymbolEnum.MINUS);
        return Message.ok(statementBalanceSheetItemMapper.deleteById(id) > 0);
    }

    @Override
    public Message<StatementSubjectBalance> getSubjectBalance(StatementSubjectBalance params) {
        LambdaQueryWrapper<StatementSubjectBalance> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementSubjectBalance::getBookId, params.getBookId());
        lqw.eq(StringUtils.isNotBlank(params.getSubjectCode()), StatementSubjectBalance::getSubjectCode, params.getSubjectCode());
        lqw.eq(StringUtils.isNotBlank(params.getSourceId()), StatementSubjectBalance::getSourceId, params.getSourceId());
        lqw.eq(StringUtils.isNotBlank(params.getYearPeriod()), StatementSubjectBalance::getYearPeriod, params.getYearPeriod());
        return Message.ok(subjectBalanceMapper.selectOne(lqw));
    }

    /**
     * 获取配置
     *
     * @return 结果
     */
    @Override
    public Message<List<StatementRules>> getRules(String itemCode) {
        LambdaQueryWrapper<StatementRules> lqw = Wrappers.lambdaQuery();
        lqw.eq(StatementRules::getItemCode, itemCode);
        lqw.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        lqw.orderByAsc(StatementRules::getSubjectCode);
        List<StatementRules> rules = rulesMapper.selectList(lqw);
        return Message.ok(rules);
    }

    @Override
    public void updateRuleBalance(StatementSubjectBalance subjectBalance, StatementRules statementRules) {
        if (subjectBalance != null) {
            statementRules.setOpeningYearBalance(subjectBalance.getOpeningYearBalanceDebit().subtract(subjectBalance.getOpeningYearBalanceCredit()));
            statementRules.setClosingBalance(subjectBalance.getBalance());
        } else {
            statementRules.setOpeningYearBalance(BigDecimal.ZERO);
            statementRules.setClosingBalance(BigDecimal.ZERO);
        }
    }

    /**
     * 刷新信息项对应的余额数据
     *
     * @param items      信息项组
     * @param bookId     所属账簿
     * @param yearPeriod 账期
     */
    @Override
    public void refreshItemsBalance(List<StatementBalanceSheetItem> items,
                                    String bookId, String yearPeriod) {
        // 方便更新参数
        Map<String, StatementBalanceSheetItem> mapSheet = items.stream()
                .collect(Collectors.toMap(StatementBalanceSheetItem::getItemCode, item -> item));
        List<String> itemCodes = items.stream().map(StatementBalanceSheetItem::getItemCode).toList();
        // 规则查询
        LambdaQueryWrapper<StatementRules> lqwRule = Wrappers.lambdaQuery();
        lqwRule.in(StatementRules::getItemCode, itemCodes);
        lqwRule.eq(StatementRules::getBookId, bookId);
        lqwRule.eq(StatementRules::getType, StatementTypeEnum.balance_sheet.name());
        List<StatementRules> rules = rulesMapper.selectList(lqwRule);
        List<String> subjectCodes = rules.stream().map(StatementRules::getSubjectCode).toList();
        if(CollectionUtils.isNotEmpty(subjectCodes)) {
	        // 查询科目余额
	        LambdaQueryWrapper<StatementSubjectBalance> lqwSubject = Wrappers.lambdaQuery();
	        lqwSubject.in(StatementSubjectBalance::getSubjectCode, subjectCodes);
	        lqwSubject.eq(StatementSubjectBalance::getBookId, bookId);
	        lqwSubject.eq(StatementSubjectBalance::getYearPeriod, yearPeriod);
	        List<StatementSubjectBalance> subjectBalances = subjectBalanceMapper.selectList(lqwSubject);
	        Map<String, StatementSubjectBalance> subjectMap = subjectBalances.stream()
	                .collect(Collectors.toMap(StatementSubjectBalance::getSubjectCode, item -> item));
	        // 更新对应规则的余额和报表余额
	        for (StatementRules statementRules : rules) {
	            StatementSubjectBalance subjectBalance = subjectMap.get(statementRules.getSubjectCode());
	            updateRuleBalance(subjectBalance, statementRules);
	            StatementBalanceSheetItem balanceSheet = mapSheet.get(statementRules.getItemCode());
	            if (StatementSymbolEnum.PLUS.getValue().equals(statementRules.getSymbol())) {
	                balanceSheet.setInitialBalance(balanceSheet.getInitialBalance().add(statementRules.getOpeningYearBalance()));
	                balanceSheet.setCurrentBalance(balanceSheet.getCurrentBalance().add(statementRules.getClosingBalance()));
	            } else {
	                balanceSheet.setInitialBalance(balanceSheet.getInitialBalance().subtract(statementRules.getOpeningYearBalance()));
	                balanceSheet.setCurrentBalance(balanceSheet.getCurrentBalance().subtract(statementRules.getClosingBalance()));
	            }
	        }
        }
    }


    /**
     * 对资产/负债中的合计类节点（如：1199、1299、1399）进行数据聚合，并更新这些节点的余额。
     *
     * @param items 数据组
     * @return 结果
     */
    @Override
    public StatementBalanceSheetItemListVo insertSubtotals(List<StatementBalanceSheetItem> items) {
        if (items == null || items.isEmpty()) return null;

        // 分组（资产和负债）
        Map<String, List<StatementBalanceSheetItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(StatementBalanceSheetItem::getAssetOrLiability));

        StatementBalanceSheetItemListVo result = new StatementBalanceSheetItemListVo();

        // 资产
        List<StatementBalanceSheetItem> assetList = grouped.getOrDefault(AssetOrLiabilityEnum.asset.name(), new ArrayList<>());
        buildTreeAndSum(assetList);
        result.setAssets(assetList);

        // 负债 + 所有者权益
        List<StatementBalanceSheetItem> liabilityList = grouped.getOrDefault(AssetOrLiabilityEnum.liability.name(), new ArrayList<>());
        buildTreeAndSum(liabilityList);
        result.setLiability(liabilityList);

        return result;
    }

    /**
     * 构建树 + 递归合计
     *
     * @param flatList 扁平化数据
     */
    private void buildTreeAndSum(List<StatementBalanceSheetItem> flatList) {
        if (flatList == null || flatList.isEmpty()) return;

        // 构建子列表引用（临时构建树结构）
        Map<String, List<StatementBalanceSheetItem>> childMap = new HashMap<>();
        for (StatementBalanceSheetItem item : flatList) {
            String parentCode = item.getParentItemCode();
            if (parentCode != null && !parentCode.isBlank()) {
                childMap.computeIfAbsent(parentCode, k -> new ArrayList<>()).add(item);
            }
        }

        // 递归聚合所有合计节点（自底向上）
        Set<String> visited = new HashSet<>();
        for (StatementBalanceSheetItem item : flatList) {
            if (!visited.contains(item.getItemCode())) {
                sumRecursively(item, childMap, visited);
            }
        }

        // 聚合所有合计节点余额，
        // 递归时已经将子节点余额聚合到父节点上，这里只需要将父节点余额聚合到根节点上即可。
        final StatementBalanceSheetItem[] maxNode = {null};
        final BigDecimal[] currentAllSum = {BigDecimal.ZERO};
        final BigDecimal[] initialAllSum = {BigDecimal.ZERO};
        // 构建映射关系
        Map<String, StatementBalanceSheetItem> itemMap = flatList.stream()
                .collect(Collectors.toMap(StatementBalanceSheetItem::getItemCode, Function.identity()));

        flatList.stream().filter(item -> item.getLevel() == 1).forEach(node -> {
            // 默认定义尾号99为合计项。如：1199、1299、1399、1199_1299
            if (node.getItemCode().endsWith("99")) {
                BigDecimal currentSum = node.getCurrentBalance() != null ? node.getCurrentBalance() : BigDecimal.ZERO;
                BigDecimal initialSum = node.getInitialBalance() != null ? node.getInitialBalance() : BigDecimal.ZERO;

                // 分割拼接节点，叠加余额
                String[] codes = node.getItemCode().split("_");
                for (String code : codes) {
                    code = code.substring(0, 2) + "00";
                    StatementBalanceSheetItem parent = itemMap.get(code);
                    if (parent != null) {
                        currentSum = currentSum.add(parent.getCurrentBalance());
                        initialSum = initialSum.add(parent.getInitialBalance());
                    }
                }
                node.setCurrentBalance(currentSum);
                node.setInitialBalance(initialSum);
                initialAllSum[0] = initialAllSum[0].add(initialSum);
                currentAllSum[0] = currentAllSum[0].add(currentSum);

                // 获取最大节点，一般为总计项
                if (maxNode[0] == null || node.getItemCode().compareTo(maxNode[0].getItemCode()) > 0) {
                    maxNode[0] = node;
                }
            }
        });
        flatList.forEach(node -> {
            // 顶级节点不允许出现余额，统计项除外
            if (node.getItemCode().endsWith("00")) {
                node.setCurrentBalance(BigDecimal.ZERO);
                node.setInitialBalance(BigDecimal.ZERO);
            }
        });

        // 设置总计项余额
        if (maxNode[0] != null) {
            maxNode[0].setCurrentBalance(currentAllSum[0]);
            maxNode[0].setInitialBalance(initialAllSum[0]);
        }
    }

    /**
     * 递归合计
     *
     * @param node     当前节点
     * @param childMap 子列表引用
     * @param visited  访问过的节点
     */
    private void sumRecursively(StatementBalanceSheetItem node,
                                Map<String, List<StatementBalanceSheetItem>> childMap,
                                Set<String> visited) {
        if (visited.contains(node.getItemCode())) return;
        visited.add(node.getItemCode());
        List<StatementBalanceSheetItem> children = childMap.getOrDefault(node.getItemCode(), Collections.emptyList());

        BigDecimal currentSum = node.getCurrentBalance() != null ? node.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal initialSum = node.getInitialBalance() != null ? node.getInitialBalance() : BigDecimal.ZERO;

        for (StatementBalanceSheetItem child : children) {
            sumRecursively(child, childMap, visited);
            if (StatementSymbolEnum.PLUS.getValue().equals(child.getSymbol())) {
                currentSum = currentSum.add(
                        child.getCurrentBalance() != null ? child.getCurrentBalance() : BigDecimal.ZERO
                );
                initialSum = initialSum.add(
                        child.getInitialBalance() != null ? child.getInitialBalance() : BigDecimal.ZERO
                );
            } else {
                currentSum = currentSum.subtract(
                        child.getCurrentBalance() != null ? child.getCurrentBalance() : BigDecimal.ZERO
                );
                initialSum = initialSum.subtract(
                        child.getInitialBalance() != null ? child.getInitialBalance() : BigDecimal.ZERO
                );
            }
        }

        node.setCurrentBalance(currentSum);
        node.setInitialBalance(initialSum);
    }

    /**
     * 更新行号，保证插入的行号，不影响到原有布局。
     *
     * @param dto    更新参数
     * @param symbol 操作数
     */
    private void updateSortIndex(StatementBalanceSheetItem dto, StatementSymbolEnum symbol) {
        // 未发生变动的序号，不重复更新，减少压力
        if (!StringUtils.isBlank(dto.getId())) {
            StatementBalanceSheetItem balanceSheet = statementBalanceSheetItemMapper.selectById(dto.getId());
            if (balanceSheet != null && balanceSheet.getSortIndex().equals(dto.getSortIndex())) {
                return;
            }
        }

        LambdaQueryWrapper<StatementBalanceSheetItem> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(StatementBalanceSheetItem::getBookId, dto.getBookId())
                .ge(StatementBalanceSheetItem::getSortIndex, dto.getSortIndex())
                .eq(StatementBalanceSheetItem::getAssetOrLiability, dto.getAssetOrLiability())
                .ne(StringUtils.isNotBlank(dto.getId()), StatementBalanceSheetItem::getId, dto.getId())
                .orderByAsc(StatementBalanceSheetItem::getSortIndex);
        List<StatementBalanceSheetItem> balanceSheets = statementBalanceSheetItemMapper.selectList(queryWrapper);
        int sortIndex = dto.getSortIndex();
        for (StatementBalanceSheetItem balanceSheet : balanceSheets) {
            if (symbol.equals(StatementSymbolEnum.PLUS)) {
                balanceSheet.setSortIndex(++sortIndex);
            } else {
                balanceSheet.setSortIndex(--sortIndex);
            }
        }
        if (!balanceSheets.isEmpty()) {
            statementBalanceSheetItemMapper.updateBatchById(balanceSheets);
        }
    }

    /**
     * 获取当前账期报表ID
     *
     * @param bookId 账簿
     * @return StatementBalanceSheet
     */
    private StatementBalanceSheet getBalanceSheetCurrentPeriod(String bookId) {
        String currentTerm = configSysService.getCurrentTerm(bookId);
        LambdaQueryWrapper<StatementBalanceSheet> lqwBalanceSheet = Wrappers.lambdaQuery();
        lqwBalanceSheet.eq(StatementBalanceSheet::getBookId, bookId);
        lqwBalanceSheet.eq(StatementBalanceSheet::getYearPeriod, currentTerm);
        lqwBalanceSheet.eq(StatementBalanceSheet::getPeriodType, StatementPeriodTypeEnum.MONTH.getValue());
        return balanceSheetMapper.selectOne(lqwBalanceSheet);
    }

}
