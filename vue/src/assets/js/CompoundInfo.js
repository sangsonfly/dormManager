import request from "@/utils/request";

const {ElMessage} = require("element-plus");

export default {
    name: "CompoundInfo",
    components: {},
    data() {
        return {
            loading: true,
            disabled: false,
            judge: false,
            dialogVisible: false,
            search: "",
            currentPage: 1,
            pageSize: 10,
            total: 0,
            tableData: [],
            form: {
                compoundName: "",
                campus: "",
                compoundDetail: "",
            },
            campusOptions: [],
            rules: {
                compoundName: [
                    {required: true, message: "请输入园区名称", trigger: "blur"},
                ],
                campus: [
                    {required: true, message: "请选择校区", trigger: "change"},
                ],
                compoundDetail: [
                    {required: true, message: "请输入备注", trigger: "blur"},
                ],
            },
        };
    },
    created() {
        this.load();
        this.loadCampusOptions();
        this.loading = true;
        setTimeout(() => {
            //设置延迟执行
            this.loading = false;
        }, 1000);
    },
    methods: {
        async load() {
            request.get("/compound/find", {
                params: {
                    pageNum: this.currentPage,
                    pageSize: this.pageSize,
                    search: this.search,
                },
            }).then((res) => {
                console.log(res);
                this.tableData = res.data.records;
                this.total = res.data.total;
                this.loading = false;
            });
        },
        reset() {
            this.search = ''
            this.load();
        },
        add() {
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                this.disabled = false;
                this.form = {};
                this.judge = false;
            });
        },
        save() {
            this.$refs.form.validate((valid) => {
                if (valid) {
                    if (this.judge === false) {
                        //新增
                        request.post("/compound/add", this.form).then((res) => {
                            console.log(res);
                            if (res.code === "0") {
                                ElMessage({
                                    message: "新增成功",
                                    type: "success",
                                });
                                this.search = "";
                                this.load();
                                this.dialogVisible = false;
                            } else {
                                ElMessage({
                                    message: res.msg,
                                    type: "error",
                                });
                            }
                        });
                    } else {
                        //修改
                        request.put("/compound/update", this.form).then((res) => {
                            console.log(res);
                            if (res.code === "0") {
                                ElMessage({
                                    message: "修改成功",
                                    type: "success",
                                });
                                this.search = "";
                                this.load();
                                this.dialogVisible = false;
                            } else {
                                ElMessage({
                                    message: res.msg,
                                    type: "error",
                                });
                            }
                        });
                    }
                }
            });
        },
        cancel() {
            this.$refs.form.resetFields();
            this.dialogVisible = false;
        },
        handleEdit(row) {
            //修改
            this.judge = true;
            this.dialogVisible = true;
            this.$nextTick(() => {
                this.$refs.form.resetFields();
                // 深拷贝
                this.form = JSON.parse(JSON.stringify(row));
                this.disabled = true;
            });
        },
        handleDelete(compoundId) {
            console.log(compoundId);
            request.delete("/compound/delete/" + compoundId).then((res) => {
                if (res.code === "0") {
                    ElMessage({
                        message: "删除成功",
                        type: "success",
                    });
                    this.search = "";
                    this.load();
                } else {
                    ElMessage({
                        message: res.msg,
                        type: "error",
                    });
                }
            });
        },
        handleSizeChange(pageSize) {
            //改变每页个数
            this.pageSize = pageSize;
            this.load();
        },
        handleCurrentChange(pageNum) {
            //改变页码
            this.currentPage = pageNum;
            this.load();
        },
        loadCampusOptions() {
            // 从后端获取校区列表
            request.get("/compound/getAllCampus").then((res) => {
                if (res.code == 200 || res.code == 0) {
                    this.campusOptions = res.data;
                } else {
                    this.$message.error("获取校区列表失败: " + res.msg);
                    this.campusOptions = ['东校区', '西校区', '南校区', '北校区'];
                }
            }).catch(() => {
                // 如果请求失败，使用默认值
                this.campusOptions = ['东校区', '西校区', '南校区', '北校区'];
            });
        },
    },
}; 