package com.sxj.supervisor.website.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.csource.common.NameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;

import com.sxj.file.common.LocalFileUtil;
import com.sxj.file.fastdfs.IFileUpLoad;
import com.sxj.spring.modules.mapper.JsonMapper;
import com.sxj.supervisor.entity.member.AccountEntity;
import com.sxj.supervisor.entity.member.MemberEntity;
import com.sxj.supervisor.enu.member.MemberCheckStateEnum;
import com.sxj.supervisor.model.login.SupervisorPrincipal;
import com.sxj.supervisor.model.member.MemberFunctionModel;
import com.sxj.supervisor.model.member.MemberQuery;
import com.sxj.supervisor.service.member.IAccountService;
import com.sxj.supervisor.service.member.IMemberFunctionService;
import com.sxj.supervisor.service.member.IMemberRoleService;
import com.sxj.supervisor.service.member.IMemberService;
import com.sxj.supervisor.website.login.SupervisorSiteToken;
import com.sxj.util.common.StringUtils;
import com.sxj.util.logger.SxjLogger;

@Controller
public class BasicController extends BaseController {

	@Autowired
	private IMemberFunctionService functionService;

	@Autowired
	private IMemberService memberService;

	@Autowired
	private IAccountService accountService;

	@Autowired
	private IMemberRoleService roleService;

	@Autowired
	private IFileUpLoad fastDfsClient;

	@RequestMapping("index")
	public String ToIndex(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute("userinfo") == null) {
			return LOGIN;
		} else {
			return "redirect:" + getBasePath(request) + "member/memberInfo.htm";
		}

	}

	@RequestMapping("to_login")
	public String ToLogin() {
		return LOGIN;
	}

	@RequestMapping("logout")
	public String logout(HttpServletRequest request) {
		Subject currentUser = SecurityUtils.getSubject();
		currentUser.logout();
		return "redirect:" + getBasePath(request) + "to_login.htm";

	}

	@RequestMapping("error")
	public String ToError() {
		return "site/500";
	}

	@RequestMapping("404")
	public String To404() {
		return "site/404";
	}

	@RequestMapping("login")
	public String login(String memberName, String accountName, String password,
			HttpSession session, HttpServletRequest request, ModelMap map) {
		map.put("accountName", accountName);
		map.put("memberName", memberName);
		SupervisorSiteToken token = null;
		SupervisorPrincipal userBean = null;
		if (StringUtils.isNotEmpty(memberName)
				&& StringUtils.isNotEmpty(accountName)) {
			AccountEntity account = accountService
					.getAccountByName(accountName);
			if (account == null) {
				map.put("amessage", "会员子账户不存在");
				return LOGIN;
			}
			String memberNo = account.getParentId();
			MemberEntity member = memberService.memberInfo(memberNo);
			if (member == null) {
				map.put("message", "会员不存在");
				return LOGIN;
			}
			if (!member.getName().equals(memberName)) {
				map.put("message", "会员名错误");
				return LOGIN;
			}
			userBean = new SupervisorPrincipal();
			userBean.setAccount(account);
			userBean.setMember(member);
			token = new SupervisorSiteToken(userBean, password);
		} else if (StringUtils.isNotEmpty(memberName)
				&& StringUtils.isEmpty(accountName)) {
			MemberEntity member = memberService.getMemberByName(memberName);
			if (member == null) {
				map.put("message", "会员不存在");
				return LOGIN;
			}
			userBean = new SupervisorPrincipal();
			userBean.setMember(member);
			token = new SupervisorSiteToken(userBean, password);
		}
		Subject currentUser = SecurityUtils.getSubject();
		try {
			currentUser.login(token);
		} catch (AuthenticationException e) {
			SxjLogger.error("登陆失败", e, this.getClass());
			map.put("pmessage", "密码错误");
			return LOGIN;

		}
		if (currentUser.isAuthenticated()) {
			session.setAttribute("userinfo", userBean);
			return "redirect:" + getBasePath(request) + "member/memberInfo.htm";
		} else {
			map.put("message", "登陆失败");
			return LOGIN;
		}
	}

	/**
	 * 左侧菜单
	 * 
	 * @param map
	 * @return
	 */
	@RequestMapping("menu")
	public String ToMenu(HttpServletRequest request, ModelMap map) {
		HttpSession session = request.getSession(false);
		if (session.getAttribute("userinfo") == null) {
			return LOGIN;
		}
		SupervisorPrincipal userBean = (SupervisorPrincipal) session
				.getAttribute("userinfo");
		if (userBean.getMember() != null && userBean.getAccount() == null) {
			if (userBean.getMember().getCheckState()
					.equals(MemberCheckStateEnum.certified)) {
				List<MemberFunctionModel> list = functionService
						.queryFunctions();
				map.put("list", list);
			}
		} else if (userBean.getMember() != null
				&& userBean.getAccount() != null) {
			List<MemberFunctionModel> list = roleService
					.getRoleFunctions(userBean.getAccount().getId());
			map.put("list", list);
		}
		return "site/menu";
	}

	@RequestMapping("upload")
	public void uploadFile(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!(request instanceof DefaultMultipartHttpServletRequest)) {
			return;
		}
		DefaultMultipartHttpServletRequest re = (DefaultMultipartHttpServletRequest) request;
		Map<String, MultipartFile> fileMaps = re.getFileMap();
		Collection<MultipartFile> files = fileMaps.values();
		List<String> fileIds = new ArrayList<String>();
		for (MultipartFile myfile : files) {
			if (myfile.isEmpty()) {
				System.err.println("文件未上传");
			} else {
				String fileId = fastDfsClient.uploadFile(myfile.getBytes(),
						myfile.getOriginalFilename());
				fileIds.add(fileId);
			}
		}
		map.put("fileIds", fileIds);
		String res = JsonMapper.nonDefaultMapper().toJson(map);
		response.setContentType("text/plain;UTF-8");
		PrintWriter out = response.getWriter();
		out.print(res);
		out.flush();
		out.close();
	}

	/**
	 * 甲方联想
	 * @param request
	 * @param response
	 * @param keyword
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("autoCompleA")
	public @ResponseBody Map<String, String> autoCompleA(
			HttpServletRequest request, HttpServletResponse response,
			String keyword) throws IOException {
		MemberQuery mq = new MemberQuery();
		if (keyword != "" && keyword != null) {
			mq.setMemberName(keyword);
		}
		mq.setMemberType(0);
		List<MemberEntity> list = memberService.queryMembers(mq);
		List strlist = new ArrayList();
		String sb = "";
		for (MemberEntity memberEntity : list) {
			sb = "{\"title\":\"" + memberEntity.getName() + "\",\"result\":\""
					+ memberEntity.getMemberNo() + "\"}";
			strlist.add(sb);
		}
		String json = "{\"data\":" + strlist.toString() + "}";
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(json);
		out.flush();
		out.close();
		return null;
	}
	
	/**
	 * 乙方联想
	 * @param request
	 * @param response
	 * @param keyword
	 * @return
	 * @throws IOException
	 */
	@RequestMapping("autoCompleB")
	public @ResponseBody Map<String, String> autoCompleB(
			HttpServletRequest request, HttpServletResponse response,
			String keyword) throws IOException {
		MemberQuery mq = new MemberQuery();
		if (keyword != "" && keyword != null) {
			mq.setMemberName(keyword);
		}
		mq.setMemberTypeB(0);
		List<MemberEntity> list = memberService.queryMembers(mq);
		List strlist = new ArrayList();
		String sb = "";
		for (MemberEntity memberEntity : list) {
			sb = "{\"title\":\"" + memberEntity.getName() + "\",\"result\":\""
					+ memberEntity.getMemberNo() + "\"}";
			strlist.add(sb);
		}
		String json = "{\"data\":" + strlist.toString() + "}";
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(json);
		out.flush();
		out.close();
		return null;
	}
	@RequestMapping("filesort")
	public @ResponseBody List<String> fileSort(String fileId)
			throws IOException {
		List<String> sortFile = new ArrayList<String>();
		try {
			String[] fileids = fileId.split(",");
			Map<String, String> nameMap = new TreeMap<String, String>();
			for (int i = 0; i < fileids.length; i++) {
				List<NameValuePair> values = fastDfsClient
						.getMetaList(fileids[i]);
				String value = values.get(0).getValue();
				nameMap.put(fileids[i], value);
			}
			List<Map.Entry<String, String>> mappingList = null;
			// 通过ArrayList构造函数把map.entrySet()转换成list
			mappingList = new ArrayList<Map.Entry<String, String>>(
					nameMap.entrySet());
			// 通过比较器实现比较排序
			Collections.sort(mappingList,
					new Comparator<Map.Entry<String, String>>() {
						public int compare(Map.Entry<String, String> mapping1,
								Map.Entry<String, String> mapping2) {
							return mapping1.getValue().compareTo(
									mapping2.getValue());
						}
					});
			for (Map.Entry<String, String> mapping : mappingList) {
				sortFile.add(mapping.getKey());
			}
			// Map<String, Object> map = new HashMap<String, Object>();
			// map.put("", sortFile);
		} catch (Exception e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
		}
		return sortFile;

	}
}