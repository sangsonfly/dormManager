<template>
  <div>
    <el-breadcrumb separator-icon="ArrowRight" style="margin: 16px">
      <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>申请报修</el-breadcrumb-item>
    </el-breadcrumb>
    <el-card style="margin: 15px; min-height: calc(100vh - 111px)">
      <!-- 没有宿舍时的提示 -->
      <div v-if="!hasRoom" style="margin-bottom: 20px;">
        <el-alert
          title="宿舍状态提醒"
          description="您当前没有宿舍。如需帮助请联系宿管。"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
      
      <div class="table-toolbar">
        <h3 class="table-title">报修申请记录</h3>
        <el-button v-if="hasRoom" type="primary" @click="add" icon="Plus">申请报修</el-button>
      </div>
      
      <!--    表格-->
      <el-table v-loading="loading" :data="tableData" border max-height="705" style="width: 100%">
        <el-table-column label="#" type="index"/>
        <el-table-column :show-overflow-tooltip="true" label="标题" prop="title"/>
        <el-table-column label="宿舍号" prop="dormBuildId" sortable width="150px"/>
        <el-table-column label="房间号">
          <template #default="scope">
            {{ scope.row.dormRoomId ? scope.row.dormRoomId.toString().slice(-3) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="申请人" prop="repairer" width="150px"/>
        <el-table-column
            :filter-method="filterTag"
            :filters="[
            { text: '完成', value: '完成' },
            { text: '未完成', value: '未完成' },
          ]"
            filter-placement="bottom-end"
            label="订单状态"
            prop="state"
            sortable
        >
          <template #default="scope">
            <el-tag :type="scope.row.state === '完成' ? 'success' : 'info'" disable-transitions
            >{{ scope.row.state }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="订单创建时间" prop="orderBuildTime" sortable/>
        <el-table-column label="订单完成时间" prop="orderFinishTime" sortable/>
        <!--      操作栏-->
        <el-table-column label="操作" width="130px">
          <template #default="scope">
            <el-button icon="more-filled" type="default" @click="showDetail(scope.row)"></el-button>
            <el-popconfirm v-if="scope.row.state==='未完成'" title="确认撤销该申请？" @confirm="cancelRepair(scope.row)">
              <template #reference>
                <el-button icon="Delete" type="danger"></el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <!--分页-->
      <div style="margin: 10px 0">
        <el-pagination
            v-model:currentPage="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        >
        </el-pagination>
      </div>
      <!--      弹窗-->
      <div>
        <el-dialog v-model="dialogVisible" title="操作" width="30%" @close="cancel">
          <el-form ref="form" :model="form" :rules="rules" label-width="120px">
            <el-form-item label="楼宇号" prop="dormBuildId" style="margin-bottom: 27px">
              <el-input v-model="form.dormBuildId" disabled style="width: 80%">{{ this.room.dormBuildId }}</el-input>
            </el-form-item>
            <el-form-item label="房间号" prop="dormRoomId" style="margin-bottom: 27px">
              <el-input v-model="form.displayRoomId" disabled style="width: 80%"></el-input>
            </el-form-item>
            <el-form-item label="申请人" prop="repairer">
              <el-input v-model="form.repairer" disabled style="width: 80%">{{ this.name }}</el-input>
            </el-form-item>
            <el-form-item label="标题" prop="title" style="margin-bottom: 27px">
              <el-input v-model="form.title" clearable style="width: 80%"></el-input>
            </el-form-item>
            <el-form-item label="内容" prop="content">
              <el-input
                  v-model="form.content"
                  :autosize="{ minRows: 3, maxRows: 10 }"
                  autosize
                  clearable
                  style="width: 80%"
                  type="textarea"
              ></el-input>
            </el-form-item>
          </el-form>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="cancel">取 消</el-button>
              <el-button type="primary" @click="save">确 定</el-button>
            </span>
          </template>
        </el-dialog>
        <!--   内容详情弹窗-->
        <el-dialog v-model="detailDialog" title="详情" width="30%">
          <el-card>
            <div v-html="detail.content"></div>
          </el-card>
          <template #footer>
            <span class="dialog-footer">
              <el-button type="primary" @click="closeDetails">确 定</el-button>
            </span>
          </template>
        </el-dialog>
      </div>
    </el-card>
  </div>
</template>
<script src="@/assets/js/ApplyRepairInfo.js"></script>
<style scoped>
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.table-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
</style>