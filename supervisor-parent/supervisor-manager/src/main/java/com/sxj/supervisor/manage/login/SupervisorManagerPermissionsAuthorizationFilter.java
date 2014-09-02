package com.sxj.supervisor.manage.login;

import java.io.IOException;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.PermissionsAuthorizationFilter;

import com.sxj.util.common.StringUtils;

public class SupervisorManagerPermissionsAuthorizationFilter extends
		PermissionsAuthorizationFilter {

	// @Resource
	// private CacheManager shiroCacheManager;

	@Override
	public boolean isAccessAllowed(ServletRequest request,
			ServletResponse response, Object mappedValue) throws IOException {

		/*
		 * Session session = user.getSession(false); Cache<Object, Object> cache
		 * = shiroCacheManager.getCache(GlobalStatic.authenticationCacheName);
		 * String cachedSessionId =
		 * cache.get(GlobalStatic.authenticationCacheName
		 * +"-"+shiroUser.getAccount()).toString(); String sessionId=(String)
		 * session.getId(); if(!sessionId.equals(cachedSessionId)){
		 * user.logout(); }
		 */

		HttpServletRequest req = (HttpServletRequest) request;
		HttpSession session = req.getSession(false);
		String functionId = req.getParameter("functionId");
		session.setAttribute("functionId", functionId);

		Subject subject = getSubject(request, response);
		String uri = req.getRequestURI();
		String contextPath = req.getContextPath() + "/";
		int i = uri.indexOf(contextPath);
		if (i > -1) {
			uri = uri.substring(i + contextPath.length());
		}
		if (StringUtils.isBlank(uri)) {
			uri = "/";
		}
		boolean permitted = false;
		if ("/".equals(uri)) {
			permitted = true;
		} else {
			permitted = subject.isPermitted(uri);
		}
		return permitted;

	}
}
