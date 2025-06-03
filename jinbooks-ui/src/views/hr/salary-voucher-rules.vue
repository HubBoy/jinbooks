<template>
  <div class="app-container">
    <el-card class="common-card query-box">
      <div class="queryForm">
        <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
          <el-form-item label="凭证类型">
            <el-select v-model="queryParams.voucherType" placeholder="" clearable style="width: 200px">
              <el-option
                  v-for="dict in voucherTypes"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleQuery">{{ t('org.button.query') }}</el-button>
            <el-button @click="resetQuery">{{ t('org.button.reset') }}</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    <el-card class="common-card">
      <div class="btn-form">
        <el-button
            type="primary"
            @click="handleAdd"
        >{{ t('org.button.add') }}
        </el-button>
        <el-button
            type="danger"
            @click="onDelete"
            :disabled="ids.length === 0"
        >{{ t('org.button.deleteBatch') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="dataList" @selection-change="handleSelectionChange" border>
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column prop="voucherType" label="凭证类型" align="left" min-width="50"
                         :show-overflow-tooltip="true">
          <template #default="scope">
            <dict-tag-number :options="voucherTypes" :value="scope.row.voucherType"/>
          </template>
        </el-table-column>
        <el-table-column prop="wordHead" label="凭证字" align="left" min-width="50"
                         :show-overflow-tooltip="true">
          <template #default="scope">
            <dict-tag :options="wordHeads" :value="scope.row.wordHead"/>
          </template>
        </el-table-column>
<!--        <el-table-column prop="summary" label="摘要" align="left" min-width="100"
                         :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="direction" label="借/贷" align="left" min-width="50">
          <template #default="scope">
            <el-tag type="warning" v-if="scope.row.direction == 0">{{ t('subjectDirectionNone') }}</el-tag>
            <el-tag type="warning" v-if="scope.row.direction == 1">{{ t('subjectDebit') }}</el-tag>
            <el-tag type="success" v-if="scope.row.direction == 2">{{ t('subjectCredit') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subjectName" label="科目" align="left" min-width="100"
                         :show-overflow-tooltip="true">
            <template #default="scope">
              {{ scope.row.subjectCode+" "+scope.row.subjectName }}
            </template>
        </el-table-column>-->
<!--        <el-table-column prop="status" :label="t('org.status')" align="center" min-width="40">
          <template #default="scope">
                <span v-if="scope.row.status === 1"><el-icon color="green"><SuccessFilled
                    class="success"/></el-icon></span>
            <span v-if="scope.row.status === 0"><el-icon color="#808080"><CircleCloseFilled/></el-icon></span>
          </template>
        </el-table-column>-->
        <el-table-column prop="status" label="状态" align="center" min-width="20">
          <template #default="scope">
            <el-switch
                v-model="scope.row.status"
                inline-prompt
                active-text="启用"
                inactive-text="禁用"
                :active-value="1"
                :inactive-value="0"
                style="--el-switch-on-color: #13ce66;"
                @change="handleStatusChange(scope.row, $event)"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('jbx.text.action')" width="80" align="center">
          <template #default="scope">
            <el-tooltip content="编辑">
              <el-button link icon="Edit" @click="handleUpdate(scope.row)"></el-button>
            </el-tooltip>
            <el-tooltip content="移除">
              <el-button link icon="Delete" type="danger" @click="onDelete(scope.row)"></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNumber"
          v-model:limit="queryParams.pageSize"
          :page-sizes="queryParams.pageSizeOptions"
          @pagination="getList"
      />
    </el-card>
<!--    <edit-form :title="title" :open="open"
               :form-id="id"
               :dept-options="deptOptions"
               :voucher-types="voucherTypes"
               :word-heads="wordHeads"
               @dialogOfClosedMethods="dialogOfClosedMethods"></edit-form>-->
    <template-form :title="title" :open="open"
                   :dept-options="deptOptions"
                   :form-id="id"
                   :word-heads="wordHeads"
                   :voucher-types="voucherTypes"
                   @dialogOfClosedMethods="dialogOfClosedMethods">
    </template-form>
  </div>
</template>

<script setup lang="ts">
import {ElForm} from "element-plus";
import {useI18n} from "vue-i18n";
import {getCurrentInstance, reactive, ref, toRefs} from "vue";
import {delRule, pageList, changeStatus} from "@/api/system/hr/voucher-rule";
import editForm from "./salary-voucher-rules/edit.vue"
import templateForm from "./salary-voucher-rules/template.vue"
import {getTree} from "@/api/system/standard/standard-subject";
import bookStore from "@/store/modules/bookStore";
import DictTagNumber from "@/components/DIctTagNumber/index.vue";
import modal from "@/plugins/modal";
import * as GendersEnum from "@/utils/enums/GendersEnum";


const {proxy} = getCurrentInstance()!;
const {voucherTypes, wordHeads}
    = proxy?.useDict( "voucherTypes", "wordHeads");
const currBookStore = bookStore()

const {t} = useI18n()
const data: any = reactive({
  queryParams: {
    pageNumber: 1,
    pageSize: 10,
    pageSizeOptions: [10, 20, 50]
  },
  rules: {
  },
});

const {queryParams, rules} = toRefs(data);
const dataList = ref([]);
const loading = ref(true);
const total = ref(0);
const title = ref("");
const open = ref(false);
const id: any = ref(undefined);
const ids = ref([]);
const selectionlist: any = ref<any>([]);
const deptOptions: any = ref<any[]>([]);
const subjectKeyItem = ref<any>({})
const subjectKeyIdItem = ref<any>({})

function getList() {
  pageList(queryParams.value).then((response: any) => {
    dataList.value = response.data.records;
    total.value = response.data.total;
    loading.value = false;
  })
}

/** 新增按钮操作 */
function handleAdd() {
  title.value = "新增凭证模板";
  id.value = undefined;
  open.value = true;
}

/*关闭抽屉*/
function dialogOfClosedMethods(val: any): any {
  open.value = false;
  id.value = undefined;
  if (val) {
    getList();
  }
}

interface TreeNode {
  id: string | number; // 根据你的数据实际情况调整类型
  children?: TreeNode[]; // 子节点可能不存在
}

function getSubjectTree(): any {
  getTree({bookId: currBookStore.bookId}).then((response: { data: TreeNode[] }) => {
    deptOptions.value = response.data;
  });
}

// 多选框选中数据
function handleSelectionChange(selection: any) {
  selectionlist.value = selection;
  ids.value = selectionlist.value.map((item: any) =>  item.id);
}

function handleUpdate(row: any) {
  id.value = row.id;
  title.value = "编辑凭证规则"
  open.value = true;
}

function onDelete(row: any) {
  const _ids = row.id ? [row.id] : ids.value;
  modal.confirm('是否确认删除？').then(function () {
    return delRule({listIds: _ids});
  }).then((res: any) => {
    if (res.code === 0) {
      getList();
      modal.msgSuccess(t('jbx.alert.delete.success'));
    } else {
      modal.msgError(res.message);
    }
  }).catch(() => {
  });
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
function resetQuery() {
  queryParams.value.voucherType = undefined;
  handleQuery();
}

/* 处理状态变更 */
function handleStatusChange(row: any, newStatus: number) {
  // 保存之前的状态用于恢复
  const oldStatus = newStatus === 1 ? 0 : 1;

  // 根据新状态决定要执行的操作
  if (newStatus === 1) {
    // 用户正在尝试启用
    modal
        .confirm("是否启用该模板？")
        .then(function () {
          return changeStatus({id: row.id, status: newStatus});
        })
        .then((res) => {
          if (res.code === 0) {
            // 成功，保持当前状态
          } else {
            // 失败，回退状态
            row.status = oldStatus;
            modal.msgError(res.message);
          }
        })
        .catch(() => {
          // 用户取消，回退状态
          row.status = oldStatus;
        });
  } else {
    // 用户正在尝试禁用
    modal
        .confirm("是否确认禁用该模板？")
        .then(function () {
          return changeStatus({id: row.id, status: newStatus});
        })
        .then((res) => {
          if (res.code === 0) {
            // 成功，保持当前状态
          } else {
            // 失败，回退状态
            row.status = oldStatus;
            modal.msgError(res.message);
          }
        })
        .catch(() => {
          // 用户取消，回退状态
          row.status = oldStatus;
        });
  }
}

getList();
getSubjectTree();
</script>

<style lang="scss" scoped>
.btn-form {
  margin-bottom: 10px;
}

.common-card {
  margin-bottom: 15px;
}

.app-container {
  padding: 0;
  background-color: #f5f7fa;
}
</style>
