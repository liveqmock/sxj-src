package com.sxj.finance.website.controller;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.sxj.finance.enu.member.MemberTypeEnum;
import com.sxj.finance.model.finance.FinancePrincipal;

public class BaseController {

	public static final String LOGIN = "site/login";

	public static final String INDEX = "site/index";

	public static final String SPRING_SECURITY_CONTEXT = "SPRING_SECURITY_CONTEXT";

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setLenient(false);
		binder.registerCustomEditor(Date.class, new CustomDateEditor(
				dateFormat, false));
	}

	protected String getBasePath(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName() + ":"
				+ request.getServerPort() + request.getContextPath() + "/";
	}

	protected FinancePrincipal getLoginInfo(HttpSession session) {
		Object object = session.getAttribute("userinfo");
		if (object != null) {
			FinancePrincipal userBean = (FinancePrincipal) object;
			return userBean;
		} else {
			return null;
		}

	}

}
