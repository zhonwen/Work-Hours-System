<template>
  <div class="page-root">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">请假类型</h2>
        <el-button type="primary" :icon="Plus" @click="handleCreate">新建类型</el-button>
      </div>
      <div class="header-right">
        <el-input
          v-model="searchForm.name"
          placeholder="搜索名称"
          clearable
          style="width: 200px; margin-right: 12px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 120px; margin-right: 12px"
          @change="handleSearch"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="list" stripe v-loading="loading" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" min-width="120" />
        <el-table-column prop="code" label="编码" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="searchForm.pageNum"
          v-model:page-size="searchForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="如：病假" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="formData.code" placeholder="如：SICK" :disabled="!!formData.id" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getLeaveTypeList,
  createLeaveType,
  updateLeaveType,
  deleteLeaveType,
  type LeaveTypeInfo
} from '@/api/leaveType'

const loading = ref(false)
const list = ref<LeaveTypeInfo[]>([])
const total = ref(0)
const searchForm = reactive({
  name: '',
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  sort: 0,
  status: 1,
  remark: ''
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '只能包含大写字母和下划线', trigger: 'blur' }
  ]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res: any = await getLeaveTypeList(searchForm)
    list.value = res.list || res.data || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  searchForm.pageNum = 1
  fetchData()
}

const handleCreate = () => {
  dialogTitle.value = '新建请假类型'
  Object.assign(formData, { id: undefined, name: '', code: '', sort: 0, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: LeaveTypeInfo) => {
  dialogTitle.value = '编辑请假类型'
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    code: row.code,
    sort: row.sort,
    status: row.status,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (formData.id) {
        await updateLeaveType({
          id: formData.id,
          name: formData.name,
          code: formData.code,
          sort: formData.sort,
          status: formData.status,
          remark: formData.remark
        })
        ElMessage.success('更新成功')
      } else {
        await createLeaveType({
          name: formData.name,
          code: formData.code,
          sort: formData.sort,
          status: formData.status,
          remark: formData.remark
        })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      fetchData()
    } catch (e) {
      console.error(e)
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = (row: LeaveTypeInfo) => {
  ElMessageBox.confirm(`确定删除请假类型「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteLeaveType(row.id)
      ElMessage.success('删除成功')
      fetchData()
    })
    .catch(() => {})
}

onMounted(fetchData)
</script>

<style scoped>
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
