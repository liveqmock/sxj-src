package com.sxj.finance.website.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sxj.finance.entity.member.AccountEntity;
import com.sxj.finance.entity.member.MemberEntity;
import com.sxj.finance.enu.member.AccountStatesEnum;
import com.sxj.finance.enu.member.MemberCheckStateEnum;
import com.sxj.finance.enu.member.MemberStatesEnum;
import com.sxj.finance.model.finance.FinancePrincipal;
import com.sxj.finance.service.finance.IFinanceService;
import com.sxj.finance.service.member.IAccountService;
import com.sxj.finance.service.member.IMemberService;
import com.sxj.finance.website.login.FinanceSiteToken;
import com.sxj.redis.core.RQueue;
import com.sxj.redis.core.collections.RedisCollections;
import com.sxj.util.LoginToken;
import com.sxj.util.common.EncryptUtil;
import com.sxj.util.common.StringUtils;
import com.sxj.util.logger.SxjLogger;

@Controller
public class BasicController extends BaseController
{
    
    @Autowired
    private IMemberService memberService;
    
    @Autowired
    private IAccountService accountService;
    
    @Autowired
    private RedisCollections collections;
    
    @Autowired
    private IFinanceService financeService;
    
    @RequestMapping("index")
    public String ToIndex(HttpServletRequest request)
    {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userinfo") == null)
        {
            return LOGIN;
        }
        else
        {
            FinancePrincipal info = getLoginInfo(session);
            if (info.getMember() != null)
            {
                String function = request.getParameter("function");
                if (StringUtils.isNotEmpty(function))
                {
                    return "redirect:" + getBasePath(request)
                            + "member/info.htm?function=" + function;
                }
                else
                {
                    return "redirect:" + getBasePath(request)
                            + "member/info.htm?function=1";
                }
            }
            else
            {
                return LOGIN;
            }
            
        }
        
    }
    
    @RequestMapping("error")
    public String ToError()
    {
        return "site/500";
    }
    
    @RequestMapping("404")
    public String To404()
    {
        return "site/404";
    }
    
    @RequestMapping("to_login")
    public String ToLogin(String member, String token, String payId,
            HttpServletRequest request, HttpSession session, ModelMap map)
            throws Exception
    {
        FinancePrincipal userBean = new FinancePrincipal();
        String retUrl = request.getHeader("Referer");
        System.out.println("-----" + retUrl + "-----");
        MemberEntity memberInfo = memberService.memberInfo(member);
        map.put("member", member);
        map.put("token", token);
        map.put("payId", payId);
        if (memberInfo == null)
        {
            return LOGIN;
        }
        if (memberInfo.getFinanceState() == 1)
        {
            LoginToken newToken = new LoginToken();
            newToken.setMemberNo(memberInfo.getMemberNo());
            newToken.setMemberName(memberInfo.getName());
            newToken.setPassword(memberInfo.getPassword());
            String tokenMd5 = EncryptUtil.md5Hex(newToken.toString());
            if (!tokenMd5.equals(token))
            {
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
            //Subject currentUser = SecurityUtils.getSubject();
            Subject currentUser2 = SecurityUtils.getSubject();
            try
            {
                userBean.setMember(memberInfo);
                FinanceSiteToken siteToken = new FinanceSiteToken(userBean,
                        memberInfo.getPassword());
                //currentUser.logout();
                currentUser2.login(siteToken);
                memberService.updateMenberLoginDate(memberInfo.getMemberNo());
            }
            catch (AuthenticationException e)
            {
                SxjLogger.error("登陆错误", e, this.getClass());
                map.put("pmessage", "登陆错误");
                return LOGIN;
            }
            if (currentUser2.isAuthenticated())
            {
                session.setAttribute("userinfo", userBean);
                if (StringUtils.isNotEmpty(payId))
                {
                    RQueue<Map<String, Object>> queue = collections.getQueue(payId);
                    Map<String, Object> map1 = queue.poll();
                    financeService.setModel(map1);
                    queue.clear();
                }
                return "redirect:" + getBasePath(request)
                        + "finance/financeList.htm";
            }
            else
            {
                map.put("message", "登陆失败");
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
            
        }
        return LOGIN;
        
    }
    
    @RequestMapping("logout")
    public String logout(HttpServletRequest request)
    {
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.logout();
        return "redirect:" + getBasePath(request) + "to_login.htm";
        
    }
    
    @RequestMapping("/autoLogin")
    public String autoLogin(String member, String payId, String token,
            HttpSession session, HttpServletRequest request, ModelMap map)
    {
        try
        {
            MemberEntity memberInfo = memberService.memberInfo(member);
            if (memberInfo == null)
            {
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
            LoginToken newToken = new LoginToken();
            newToken.setMemberNo(memberInfo.getMemberNo());
            newToken.setMemberName(memberInfo.getName());
            newToken.setPassword(memberInfo.getPassword());
            String tokenMd5 = EncryptUtil.md5Hex(newToken.toString());
            if (!tokenMd5.equals(token))
            {
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
            FinancePrincipal userBean = new FinancePrincipal();
            userBean.setMember(memberInfo);
            FinanceSiteToken siteToken = new FinanceSiteToken(userBean,
                    memberInfo.getPassword());
            Subject currentUser = SecurityUtils.getSubject();
            try
            {
                //currentUser.logout();
                currentUser.login(siteToken);
                memberService.updateMenberLoginDate(memberInfo.getMemberNo());
            }
            catch (AuthenticationException e)
            {
                SxjLogger.error("登陆错误", e, this.getClass());
                map.put("pmessage", "登陆错误");
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
            if (currentUser.isAuthenticated())
            {
                session.setAttribute("userinfo", userBean);
                if (!StringUtils.isEmpty(payId))
                {
                    RQueue<Map<String, Object>> queue = collections.getQueue(payId);
                    Map<String, Object> map1 = queue.poll();
                    financeService.setModel(map1);
                    queue.clear();
                }
                return "redirect:" + getBasePath(request) + "index.htm";
            }
            else
            {
                map.put("message", "登陆失败");
                map.put("payId", payId);
                map.put("member", member);
                map.put("token", token);
                return LOGIN;
            }
        }
        catch (Exception e)
        {
            map.put("message", "登陆失败");
            map.put("payId", payId);
            map.put("member", member);
            map.put("token", token);
            SxjLogger.debug(e, e, getClass());
            return LOGIN;
        }
    }
    
    @RequestMapping("/login")
    public String login(String memberName, String accountName, String password,
            HttpSession session, HttpServletRequest request, ModelMap map)
    {
        map.put("accountName", accountName);
        map.put("memberName", memberName);
        password = EncryptUtil.md5Hex(password);
        FinanceSiteToken token = null;
        FinancePrincipal userBean = null;
        AccountEntity account = null;
        MemberEntity member = null;
        if (StringUtils.isNotEmpty(memberName)
                && StringUtils.isNotEmpty(accountName))
        {
            member = memberService.getMemberByName(memberName);
            if (member == null)
            {
                map.put("message", "会员不存在");
                return LOGIN;
            }
            if (!member.getName().equals(memberName))
            {
                map.put("message", "会员名错误");
                return LOGIN;
            }
            if (MemberCheckStateEnum.unaudited.equals(member.getCheckState()))
            {
                map.put("message", "会员未审核");
                return LOGIN;
            }
            if (MemberStatesEnum.stop.equals(member.getState()))
            {
                map.put("message", "会员已冻结");
                return LOGIN;
            }
            
            account = accountService.getAccountByName(accountName,
                    member.getMemberNo());
            if (account == null)
            {
                map.put("amessage", "会员子账户不存在");
                return LOGIN;
            }
            if (AccountStatesEnum.stop.equals(account.getState()))
            {
                map.put("amessage", "会员子账户已冻结");
                return LOGIN;
            }
            
            userBean = new FinancePrincipal();
            userBean.setAccount(account);
            userBean.setMember(member);
            token = new FinanceSiteToken(userBean, password);
        }
        else if (StringUtils.isNotEmpty(memberName)
                && StringUtils.isEmpty(accountName))
        {
            member = memberService.getMemberByName(memberName);
            if (member == null)
            {
                map.put("message", "会员不存在");
                return LOGIN;
            }
            if (MemberCheckStateEnum.unaudited.equals(member.getCheckState()))
            {
                map.put("message", "会员未审核");
                return LOGIN;
            }
            if (MemberStatesEnum.stop.equals(member.getState()))
            {
                map.put("message", "会员已冻结");
                return LOGIN;
            }
            userBean = new FinancePrincipal();
            userBean.setMember(member);
            token = new FinanceSiteToken(userBean, password);
        }
        else
        {
            map.put("message", "公司名称不能为空");
            map.put("pmessage", "密码不能为空");
            return LOGIN;
        }
        Subject currentUser = SecurityUtils.getSubject();
        try
        {
            currentUser.login(token);
            memberService.updateMenberLoginDate(member.getMemberNo());
        }
        catch (AuthenticationException e)
        {
            SxjLogger.error("登陆错误", e, this.getClass());
            map.put("pmessage", "登陆错误");
            return LOGIN;
            
        }
        if (currentUser.isAuthenticated())
        {
            session.setAttribute("userinfo", userBean);
            if (account != null)
            {
                accountService.edit_Login(account.getId());
            }
            return "redirect:" + getBasePath(request) + "index.htm";
        }
        else
        {
            map.put("message", "登陆失败");
            return LOGIN;
        }
    }
}
