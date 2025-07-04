<template>
  <div class="violation-container">
    <div class="header">
      <h2>违纪管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增违纪记录
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="请输入学生姓名或房间号"
        style="width: 300px"
        clearable
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 数据表格 -->
    <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
      <el-table-column label="房间号">
        <template #default="scope">
          {{ scope.row.dormroomId || '' }}
        </template>
      </el-table-column>
      <el-table-column prop="studentName" label="学生姓名" width="100" />
      <el-table-column prop="violationType" label="违纪类型" width="120" />
      <el-table-column prop="violationDescription" label="违纪描述" />
      <el-table-column prop="violationDate" label="违纪时间" width="160" />
      <el-table-column prop="penaltyScore" label="扣分" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reporter" label="上报人员" width="100" />
      <el-table-column prop="handler" label="处理人员" width="100" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)" :disabled="scope.row.status !== '待处理'">编辑</el-button>
          <el-button size="small" type="success" @click="handleProcess(scope.row)" :disabled="scope.row.status !== '待处理'">处理</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="showAddDialog"
      :title="isEdit ? '编辑违纪记录' : '新增违纪记录'"
      width="600px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="房间号" prop="inputRoom">
          <el-input v-model="form.inputRoom" maxlength="3" placeholder="如101, 305" />
        </el-form-item>
        <el-form-item label="宿舍楼号" prop="dormbuildId">
          <el-input v-model="form.dormbuildId" placeholder="请输入宿舍楼号" />
        </el-form-item>
        <el-form-item label="学生学号" prop="studentUsername">
          <el-input v-model="form.studentUsername" placeholder="请输入学生学号" />
        </el-form-item>
        <el-form-item label="学生姓名" prop="studentName">
          <el-input v-model="form.studentName" placeholder="请输入学生姓名" />
        </el-form-item>
        <el-form-item label="违纪类型" prop="violationType">
          <el-select v-model="form.violationType" placeholder="请选择违纪类型" style="width: 100%">
            <el-option label="使用大功率电器" value="使用大功率电器" />
            <el-option label="宿舍吵闹" value="宿舍吵闹" />
            <el-option label="晚归" value="晚归" />
            <el-option label="夜不归宿" value="夜不归宿" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="违纪描述" prop="violationDescription">
          <el-input
            v-model="form.violationDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入违纪描述"
          />
        </el-form-item>
        <el-form-item label="违纪时间" prop="violationDate">
          <el-date-picker
            v-model="form.violationDate"
            type="datetime"
            placeholder="选择违纪时间"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="扣分" prop="penaltyScore">
          <el-input-number
            v-model="form.penaltyScore"
            :min="0"
            :max="100"
            placeholder="扣分"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="上报人员" prop="reporter">
          <el-input v-model="form.reporter" readonly placeholder="自动带出当前用户" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import request from '@/utils/request'

export default {
  name: 'ViolationInfo',
  components: { Plus, Search },
  setup() {
    const loading = ref(true)
    const tableData = ref([])
    const currentPage = ref(1)
    const pageSize = ref(10)
    const total = ref(0)
    const searchQuery = ref('')
    const showAddDialog = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const user = ref(JSON.parse(sessionStorage.getItem('access-user') || '{}'))

    const form = reactive({
      id: null,
      inputRoom: '',
      dormbuildId: '',
      studentUsername: '',
      studentName: '',
      violationType: '',
      violationDescription: '',
      violationDate: '',
      penaltyScore: 0,
      reporter: '管理员' // 设置默认值
    })

    const rules = {
      inputRoom: [
        { required: true, message: '请输入房间号', trigger: 'blur' },
        { pattern: /^\d{3}$/, message: '房间号必须是3位数字', trigger: 'blur' }
      ],
      dormbuildId: [
        { required: true, message: '请输入宿舍楼号', trigger: 'blur' },
        { pattern: /^\d+$/, message: '宿舍楼号必须是数字', trigger: 'blur' }
      ],
      studentUsername: [{ required: true, message: '请输入学生学号', trigger: 'blur' }],
      studentName: [{ required: true, message: '请输入学生姓名', trigger: 'blur' }],
      violationType: [{ required: true, message: '请选择违纪类型', trigger: 'change' }],
      violationDescription: [{ required: true, message: '请输入违纪描述', trigger: 'blur' }],
      violationDate: [{ required: true, message: '请选择违纪时间', trigger: 'change' }],
      reporter: [{ required: true, message: '请输入上报人员', trigger: 'blur' }]
    }

    const fetchData = async () => {
      loading.value = true
      try {
        const res = await request.get('/violation/list', {
          params: {
            pageNum: currentPage.value,
            pageSize: pageSize.value,
            search: searchQuery.value
          }
        })
        if (res.code === '0') {
          tableData.value = res.data.records
          total.value = res.data.total
        } else {
          ElMessage.error(res.msg || '获取数据失败')
        }
      } catch (error) {
        console.error('获取数据失败:', error)
        ElMessage.error('获取数据失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      currentPage.value = 1
      fetchData()
    }

    const handleSizeChange = (val) => {
      pageSize.value = val
      fetchData()
    }

    const handleCurrentChange = (val) => {
      currentPage.value = val
      fetchData()
    }

    const resetForm = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
      // 正确获取用户信息
      const currentUser = JSON.parse(sessionStorage.getItem('access-user') || '{}')
      Object.assign(form, {
        id: null,
        inputRoom: '',
        dormbuildId: '',
        studentUsername: '',
        studentName: '',
        violationType: '',
        violationDescription: '',
        violationDate: '',
        penaltyScore: 0,
        reporter: currentUser.name || currentUser.username || '管理员'
      })
      isEdit.value = false
    }

    const handleEdit = (row) => {
      isEdit.value = true
      // 将后端字段转换为前端字段
      const editData = {
        ...row,
        inputRoom: row.dormroomId ? row.dormroomId.toString().slice(-3) : ''
      }
      Object.assign(form, editData)
      showAddDialog.value = true
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除这条记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await request.delete(`/violation/delete/${row.id}`)
          if (res.code === '0') {
            ElMessage.success('删除成功')
            fetchData()
          } else {
            ElMessage.error(res.msg || '删除失败')
          }
        } catch (error) {
          console.error('删除失败:', error)
          ElMessage.error('删除失败')
        }
      }).catch(() => {})
    }

    const handleProcess = (row) => {
      ElMessageBox.prompt('请输入处理结果', '处理违纪记录', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '处理结果不能为空'
      }).then(async ({ value }) => {
        try {
          // 正确获取用户信息
          const currentUser = JSON.parse(sessionStorage.getItem('access-user') || '{}')
          const handler = currentUser.name || currentUser.username || '管理员'
          
          const res = await request.put('/violation/process', {
            id: row.id,
            handleResult: value,
            handler: handler
          })
          if (res.code === '0') {
            ElMessage.success('处理成功')
            fetchData()
          } else {
            ElMessage.error(res.msg || '处理失败')
          }
        } catch (error) {
          console.error('处理失败:', error)
          ElMessage.error('处理失败')
        }
      }).catch(() => {})
    }

    const handleSubmit = async () => {
      if (!formRef.value) return
      try {
        console.log('提交前的表单数据:', form)
        console.log('reporter字段值:', form.reporter, '类型:', typeof form.reporter)
        await formRef.value.validate()
        
        // 构建提交给后端的数据
        const submitData = {
          id: form.id,
          dormroomId: parseInt(form.dormbuildId + form.inputRoom), // 组合楼栋号和房间号
          dormbuildId: parseInt(form.dormbuildId),
          studentUsername: form.studentUsername,
          studentName: form.studentName,
          violationType: form.violationType,
          violationDescription: form.violationDescription,
          violationDate: form.violationDate,
          penaltyScore: form.penaltyScore || 0,
          reporter: form.reporter || '管理员'
        }
        
        console.log('提交给后端的数据:', submitData)
        
        const url = isEdit.value ? '/violation/update' : '/violation/add'
        const method = isEdit.value ? 'put' : 'post'
        const res = await request[method](url, submitData)
        if (res.code === '0') {
          ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
          showAddDialog.value = false
          resetForm()
          fetchData()
        } else {
          ElMessage.error(res.msg || '操作失败')
        }
      } catch (error) {
        console.error('提交失败:', error)
        console.error('表单验证失败详情:', error.message)
        console.error('表单数据:', form)
        ElMessage.error('表单验证失败: ' + error.message)
      }
    }

    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      showAddDialog.value = true
    }

    const getStatusType = (status) => {
      switch (status) {
        case '待处理': return 'warning'
        case '已处理': return 'success'
        case '已驳回': return 'danger'
        default: return 'info'
      }
    }

    onMounted(() => {
      fetchData()
      // 正确获取用户信息
      const currentUser = JSON.parse(sessionStorage.getItem('access-user') || '{}')
      console.log('当前用户信息:', currentUser)
      form.reporter = currentUser.name || currentUser.username || '管理员'
      console.log('设置的上报人员:', form.reporter)
    })

    return {
      loading,
      tableData,
      currentPage,
      pageSize,
      total,
      searchQuery,
      showAddDialog,
      isEdit,
      form,
      formRef,
      rules,
      handleSearch,
      handleSizeChange,
      handleCurrentChange,
      handleEdit,
      handleDelete,
      handleProcess,
      handleSubmit,
      resetForm,
      getStatusType,
      handleAdd
    }
  }
}
</script>

<style scoped>
.violation-container { padding: 20px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.header h2 { margin: 0; color: #303133; }
.search-bar { margin-bottom: 20px; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; }
</style> 