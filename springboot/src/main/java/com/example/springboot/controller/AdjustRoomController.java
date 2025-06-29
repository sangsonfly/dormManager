package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.AdjustRoom;
import com.example.springboot.entity.DormRoom;
import com.example.springboot.service.AdjustRoomService;
import com.example.springboot.service.DormRoomService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/adjustRoom")
public class AdjustRoomController {

    @Resource
    private AdjustRoomService adjustRoomService;

    @Resource
    private DormRoomService dormRoomService;



    /**
     * 添加订单
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody AdjustRoom adjustRoom) {
        // 检查学生是否有宿舍
        DormRoom dormRoom = dormRoomService.judgeHadBed(adjustRoom.getUsername());
        if (dormRoom == null) {
            return Result.error("-1", "您当前没有宿舍，无法申请调宿");
        }
        // 自动生成申请时间
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        adjustRoom.setApplyTime(now.format(formatter));

        int result = adjustRoomService.addApply(adjustRoom);
        if (result == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "查询失败");
        }
    }


    /**
     * 更新订单
     */
    @PutMapping("/update/{state}")
    public Result<?> update(@RequestBody AdjustRoom adjustRoom, @PathVariable Boolean state, javax.servlet.http.HttpSession session) {
        Object userObj = session.getAttribute("User");
        Object identityObj = session.getAttribute("Identity");
        
        if (userObj == null || identityObj == null) {
            return Result.error("-1", "无权限");
        }
        
        String identity = identityObj.toString();
        
        if ("admin".equals(identity)) {
            // 管理员可以处理所有申请，不需要楼栋限制
        } else if ("dormManager".equals(identity)) {
            // 宿管只能处理自己楼栋的申请
            if (!(userObj instanceof com.example.springboot.entity.DormManager)) {
            return Result.error("-1", "无权限");
        }
        com.example.springboot.entity.DormManager manager = (com.example.springboot.entity.DormManager) userObj;
            Integer dormbuildId = manager.getDormbuildId();
            
            // 检查宿管是否有管理的楼栋
            if (dormbuildId == null) {
                return Result.error("-1", "无权限处理该申请：您没有分配管理的楼栋");
            }
            
            // 检查当前房间的楼栋
            DormRoom currentRoom = dormRoomService.checkRoomExist(adjustRoom.getCurrentRoomId());
            if (currentRoom == null) {
                return Result.error("-1", "无权限处理该申请：当前房间不存在");
            }
            
            if (currentRoom.getDormBuildId() != dormbuildId) {
                return Result.error("-1", "无权限处理该申请：当前房间不在管辖范围内");
        }
            
            // 检查目标房间的楼栋
            DormRoom targetRoom = dormRoomService.checkRoomExist(adjustRoom.getTowardsRoomId());
            if (targetRoom == null) {
                return Result.error("-1", "无权限处理该申请：目标房间不存在");
            }
            
            if (targetRoom.getDormBuildId() != dormbuildId) {
                return Result.error("-1", "无权限处理该申请：目标房间不在管辖范围内");
            }
        } else {
            return Result.error("-1", "无权限");
        }
        
        if (state) {
            int i = dormRoomService.adjustRoomUpdate(adjustRoom);
            if (i == -2) {
                return Result.error("-1", "当前床位不存在，无法调宿");
            } else if (i == -3) {
                return Result.error("-1", "目标房间不存在，无法调宿");
            } else if (i == -4) {
                return Result.error("-1", "目标床位号无效，无法调宿");
            } else if (i == -5) {
                return Result.error("-1", "目标床位已被占用，无法调宿");
            } else if (i != 1) {
                return Result.error("-1", "调宿操作失败");
            }
        }
        int i = adjustRoomService.updateApply(adjustRoom);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "更新失败");
        }
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Integer id, javax.servlet.http.HttpSession session) {
        Object userObj = session.getAttribute("User");
        Object identityObj = session.getAttribute("Identity");
        
        if (userObj == null || identityObj == null) {
            return Result.error("-1", "无权限");
        }
        
        String identity = identityObj.toString();
        
        // 查申请
        AdjustRoom adjustRoom = adjustRoomService.getById(id);
        if (adjustRoom == null) {
            return Result.error("-1", "申请不存在");
        }
        
        if ("admin".equals(identity)) {
            // 管理员可以删除所有申请，不需要楼栋限制
        } else if ("dormManager".equals(identity)) {
            // 宿管只能删除自己楼栋的申请
            if (!(userObj instanceof com.example.springboot.entity.DormManager)) {
                return Result.error("-1", "无权限");
            }
            com.example.springboot.entity.DormManager manager = (com.example.springboot.entity.DormManager) userObj;
            Integer dormbuildId = manager.getDormbuildId();
            
            // 检查当前房间的楼栋
            DormRoom currentRoom = dormRoomService.checkRoomExist(adjustRoom.getCurrentRoomId());
            if (currentRoom == null || currentRoom.getDormBuildId() != dormbuildId) {
                return Result.error("-1", "无权限删除该申请：当前房间不在管辖范围内");
        }
            
            // 检查目标房间的楼栋
            DormRoom targetRoom = dormRoomService.checkRoomExist(adjustRoom.getTowardsRoomId());
            if (targetRoom == null || targetRoom.getDormBuildId() != dormbuildId) {
                return Result.error("-1", "无权限删除该申请：目标房间不在管辖范围内");
            }
        } else {
            return Result.error("-1", "无权限");
        }
        
        int i = adjustRoomService.deleteAdjustment(id);
        if (i == 1) {
            return Result.success();
        } else {
            return Result.error("-1", "删除失败");
        }
    }

    /**
     * 查找订单
     */
    @GetMapping("/find")
    public Result<?> findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "") String search,
                              javax.servlet.http.HttpSession session) {
        // 获取用户身份和权限控制
        Object userObj = session.getAttribute("User");
        Object identityObj = session.getAttribute("Identity");
        
        if (userObj == null || identityObj == null) {
            return Result.error("-1", "无权限");
        }
        
        String identity = identityObj.toString();
        Page page;
        
        if ("admin".equals(identity)) {
            // 管理员可以看到所有申请
            page = adjustRoomService.find(pageNum, pageSize, search);
        } else if ("dormManager".equals(identity)) {
            // 宿管只能看到自己楼栋的申请
            if (!(userObj instanceof com.example.springboot.entity.DormManager)) {
            return Result.error("-1", "无权限");
        }
        com.example.springboot.entity.DormManager manager = (com.example.springboot.entity.DormManager) userObj;
            Integer dormbuildId = manager.getDormbuildId();
            page = adjustRoomService.findByBuild(pageNum, pageSize, search, dormbuildId);
        } else {
            return Result.error("-1", "无权限");
        }
        
        if (page != null) {
            return Result.success(page);
        } else {
            return Result.error("-1", "查询失败");
        }
    }

    /**
     * 根据用户名查询调宿申请（学生端使用）
     */
    @GetMapping("/findByUsername/{username}")
    public Result<?> findByUsername(@PathVariable String username,
                                   @RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize) {
        Page page = adjustRoomService.findByUsername(username, pageNum, pageSize);
        return page != null ? Result.success(page) : Result.error("-1", "查询失败");
    }

    /**
     * 学生撤销调宿申请（只能撤销自己且状态为未处理的申请）
     */
    @DeleteMapping("/studentDelete/{id}/{username}")
    public Result<?> studentDelete(@PathVariable Integer id, @PathVariable String username) {
        AdjustRoom adjustRoom = adjustRoomService.getById(id);
        if (adjustRoom == null) {
            return Result.error("-1", "申请不存在");
        }
        if (!"未处理".equals(adjustRoom.getState())) {
            return Result.error("-1", "只能撤销未处理的调宿申请");
        }
        if (!username.equals(adjustRoom.getUsername())) {
            return Result.error("-1", "无权限撤销他人申请");
        }
        int i = adjustRoomService.deleteAdjustment(id);
        return i == 1 ? Result.success() : Result.error("-1", "撤销失败");
    }
}
