<template>
  <div v-if="props.auxiliary && props.auxiliary.length > 0">
    <el-form :model="form" :rules="rules" ref="auxiliaryFormRef" label-width="68px">
      <!-- 是否禁用 -->
      <el-form-item v-for="(item, index) in auxiliary" :key="item.id"
                    :label="item.label" :prop="item.value">
        <el-select style="width: 300px" :model-value="itemValue(item).value.map((t: any) => {return t.value})"
                   multiple :multiple-limit="1" clearable placeholder="请选择"
                   @remove-tag="handleRemoveTag(item, $event)"
                   @clear="handleClear(item)">
          <el-option v-for="itemOp in listData[item.value]"
                     :label="itemOp.label"
                     :value="itemOp.value"
                     :disabled="itemOp.status === 'y'">
            <div v-if="itemOp.status === 'y'" style="color: #8c939d">{{ itemOp.label }}</div>
            <div v-else @click.stop="handleChange(item, itemOp)">{{ itemOp.label }}</div>
          </el-option>
        </el-select>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import {listAssistAcc} from "@/api/config/assistAcc";
import {reactive, ref, toRefs, watch, onMounted, computed} from "vue";
import {ElForm, FormInstance, ElMessage} from "element-plus";

const emit: any = defineEmits(['update:modelValue'])
const auxiliaryFormRef = ref<FormInstance>();

const props = defineProps({
  subjectId: {
    type: String,
    default: ''
  },
  auxiliary: {
    type: Array<any>,
    default: []
  },
  modelValue: {
    type: Array<any>,
    default: []
  },
})

const data: any = reactive({
  form: {},
  rules: {}
});

const {rules, form} = toRefs(data);
const {auxiliary, modelValue} = toRefs(props);
const listData = ref<any>({})
const values = ref<any>({})

const itemValue = (aux: any) => {
  if (!values.value[aux.value]) {
    values.value[aux.value] = {
      id: aux.value,
      value: [],
      label: aux.label,
    }
  }
  return values.value[aux.value]
}

/** 查询供应商列表 */
function getList(t: any, id: number) {
  listAssistAcc({
    pageNum: 1,
    pageSize: 1000,
    assistType: id,
    orderByColumn: "status",
    isAsc: "asc"
  }).then((res: any) => {
    listData.value[id] = res.data.records.map((t: any) => {
      return {
        label: t.assistName,
        value: t.id,
        status: t.status
      }
    })
  });
}

function handleValidate(index: number) {
  auxiliaryFormRef.value?.validate((valid, fields) => {
    if (valid) {

    } else {
      console.error(fields, rules.value, values.value)
    }
  });
}

function handleClear(item: any) {
  itemValue(item).value = []
  handleUpdate()
}

function handleRemoveTag(item: any, value: any) {
  const idx = itemValue(item).value.findIndex((t: any) => {
    return t.value === value
  })
  if (idx > -1) {
    itemValue(item).value.splice(idx, 1)
  }
  handleUpdate()
}

function handleChange(item: any, value: any) {
  const idx = itemValue(item).value.findIndex((t: any) => {
    return t.value === value.value
  })
  if (idx > -1) {
    itemValue(item).value.splice(idx, 1)
  } else {
    if (itemValue(item).value.length === 1) {
      return
    }
    itemValue(item).value.push(value)
  }
  form.value[item.value] = itemValue(item).value.join(",")
  handleUpdate()
}

function handleUpdate() {
  emit('update:modelValue', Object.values(values.value));
  handleValidate(0)
}

function watchUpdate(newVal: any) {
  if (newVal) {
    form.value.length = 0
    newVal.forEach((t: any, index: number) => {
      getList(t, t.value)
      const id = t.value
      if (t.must) {
        rules.value[id] = [
          {required: true, message: t.label + '不能为空', trigger: 'change'},
        ]
      } else {
        rules.value[id] = []
      }

      form.value[id] = ""
    })
    handleValidate(0)
  }
}

watch(() => props.modelValue, (newVal, oldVal) => {
  if (newVal) {
    values.value = {}
    newVal.forEach((t: any, index: number) => {
      values.value[t.id] = t
      form.value[t.id] = t.value ? t.value.join(",") : ''
    })
    handleValidate(0)
  }
});
watch(
    () => props.auxiliary,
    (newVal: Array<any>) => {
      rules.value.length = 0
      listData.value.length = 0
      watchUpdate(newVal)
    },
    {immediate: true}
)

onMounted(() => {
  values.value = {}
  props.modelValue.forEach((t: any, index: number) => {
    values.value[t.id] = t
  })
})
</script>