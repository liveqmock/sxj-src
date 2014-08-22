package com.sxj.supervisor.manage.controller.system;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sxj.supervisor.entity.system.FunctionEntity;
import com.sxj.supervisor.entity.system.SystemAccountEntity;
import com.sxj.supervisor.manage.controller.BaseController;
import com.sxj.supervisor.model.system.SysAccountQuery;
import com.sxj.supervisor.service.system.IFunctionService;
import com.sxj.supervisor.service.system.ISystemAccountService;
import com.sxj.util.persistent.ResultList;

@Controller
@RequestMapping("/system")
public class SystemAccountController extends BaseController
{
    
    @Autowired
    private ISystemAccountService accountService;
    
    @Autowired
    private IFunctionService functionService;
    
    @RequestMapping("account-list")
    public String getSysAccountList(SysAccountQuery query, ModelMap map)
    {
        ResultList<SystemAccountEntity> list = accountService.queryAccounts(query);
        List<FunctionEntity> functionList = functionService.queryChildrenFunctions("0");
        map.put("list", list.getResults());
        map.put("functions", functionList);
        map.put("query", query);
        return "manage/system/account-list";
        
    }
    
    @RequestMapping("account-info")
    public String getgetSysAccount(String accountId, ModelMap map)
    {
        SystemAccountEntity account = accountService.getAccount(accountId);
        map.put("account", account);
        return "manage/system/account-info";
    }
    
    @RequestMapping("to_edit")
    public String toEditAccount(String accountId, ModelMap map)
    {
        SystemAccountEntity account = accountService.getAccount(accountId);
        map.put("account", account);
        return "manage/system/account-edit";
    }
}
