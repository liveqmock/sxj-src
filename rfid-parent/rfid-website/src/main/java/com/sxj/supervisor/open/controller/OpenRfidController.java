package com.sxj.supervisor.open.controller;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

<<<<<<< HEAD
import javax.servlet.http.HttpSession;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
=======
import javax.servlet.http.HttpServletResponse;

>>>>>>> 5770d2c3ae6d77b593b2808b7959675d154c237a
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

<<<<<<< HEAD
import com.sxj.supervisor.entity.contract.ContractBatchEntity;
import com.sxj.supervisor.entity.contract.ContractEntity;
import com.sxj.supervisor.entity.member.AccountEntity;
import com.sxj.supervisor.enu.member.AccountStatesEnum;
import com.sxj.supervisor.model.contract.BatchItemModel;
import com.sxj.supervisor.model.contract.ContractBatchModel;
import com.sxj.supervisor.model.contract.ContractModel;
import com.sxj.supervisor.model.login.SupervisorPrincipal;
import com.sxj.supervisor.rfid.login.SupervisorShiroRedisCache;
import com.sxj.supervisor.rfid.login.SupervisorSiteToken;
import com.sxj.supervisor.service.open.member.IAccountService;
import com.sxj.util.common.StringUtils;
import com.sxj.util.exception.WebException;
=======
import com.sxj.spring.modules.mapper.JsonMapper;
import com.sxj.supervisor.model.open.BatchModel;
import com.sxj.supervisor.model.open.WinTypeModel;
import com.sxj.supervisor.service.rfid.open.IOpenRfidService;
import com.sxj.util.exception.ServiceException;
>>>>>>> 5770d2c3ae6d77b593b2808b7959675d154c237a

@Controller
@RequestMapping("/rfid")
public class OpenRfidController {
	@Autowired
<<<<<<< HEAD
	private IAccountService accountService;
=======
	IOpenRfidService openRfidService;
	
>>>>>>> 5770d2c3ae6d77b593b2808b7959675d154c237a

	/**
	 * 登陆
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "login")
	public int login(String userId, String password, HttpSession session)
			throws WebException {
		SupervisorSiteToken token = null;
		SupervisorPrincipal userBean = null;
		AccountEntity account = null;
		if (StringUtils.isNotEmpty(userId)) {
			account = accountService.getAccountByAccountNo(userId);
			if (account == null) {
				return 0;
			}
			if (AccountStatesEnum.stop.equals(account.getState())) {
				return 0;
			}

			userBean = new SupervisorPrincipal();
			userBean.setAccount(account);
			token = new SupervisorSiteToken(userBean, password);
		} else {
			return 0;
		}
		Subject currentUser = SecurityUtils.getSubject();
		try {
			currentUser.login(token);
			PrincipalCollection principals = currentUser.getPrincipals();
			if (userBean.getAccount() != null) {
				SupervisorShiroRedisCache.addToMap(userBean.getAccount()
						.getId(), principals);
			}
		} catch (AuthenticationException e) {
			return -1;

		}
		if (currentUser.isAuthenticated()) {
			session.setAttribute("userinfo", userBean);
			if (account != null) {
				accountService.edit_Login(account.getId());
			}
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * 注销
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "logout")
	public int logout(String userId) {
		try {
			Subject currentUser = SecurityUtils.getSubject();
			currentUser.logout();
			return 0;
		} catch (Exception e) {
			return 1;
		}
	}

	/**
	 * 获取批次信息
	 * 
	 * @param rfidNo
	 * @return
	 * @throws SQLException
	 */
	@RequestMapping(value = "info/batch/{rfidNo}")
	public @ResponseBody BatchModel getRfidBatchInfo(
			@PathVariable String rfidNo, HttpServletResponse response)
			throws SQLException {
		try {
			BatchModel model = openRfidService.getBatchByRfid(rfidNo);
			response.setContentType("application/json; charset=utf-8");
			PrintWriter out = response.getWriter();
			System.err.println(JsonMapper.nonEmptyMapper().toJson(model));
			out.print(JsonMapper.nonEmptyMapper().toJson(model));
			out.flush();
			out.close();
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	/**
	 * 获取合同规格信息
	 * 
	 * @param rfidNo
	 * @return
	 * @throws SQLException 
	 * @throws ServiceException 
	 */
	@RequestMapping(value = "info/contract/{rfidNo}")
	public @ResponseBody WinTypeModel getRfidContractInfo(
			@PathVariable String rfidNo,HttpServletResponse response) throws ServiceException, SQLException {
		
		try {
			WinTypeModel win=openRfidService.getWinTypeByRfid(rfidNo);
			response.setContentType("application/json; charset=utf-8");
			PrintWriter out = response.getWriter();
			System.err.println(JsonMapper.nonEmptyMapper().toJson(win));
			out.print(JsonMapper.nonEmptyMapper().toJson(win));
			out.flush();
			out.close();
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	/**
	 * 发货
	 * 
	 * @param rfidNo
	 * @return
	 */
	@RequestMapping(value = "send/{rfidNo}")
	public @ResponseBody Map<String, Object> sendGoods(
			@PathVariable String rfidNo) {
		return null;

	}

	/**
	 * 验收
	 * 
	 * @param rfidNo
	 * @return
	 */
	@RequestMapping(value = "check/{rfidNo}")
	public @ResponseBody Map<String, Object> checkAndAccept(
			@PathVariable String rfidNo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("state", 1);
		map.put("contractNo", "CT1410250001");
		map.put("batchNo", "00001");
		return map;
	}

	/**
	 * 质检
	 * 
	 * @param rfidNo
	 * @return
	 */
	@RequestMapping(value = "test/{rfidNo}")
	public @ResponseBody Map<String, Object> testRfid(
			@PathVariable String rfidNo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("contractNo", "CT1410250001");
		String[] batchNos = new String[2];
		batchNos[0] = "AAAA0001";
		batchNos[1] = "AAAA0002";
		map.put("batchNo", batchNos);
		return map;

	}

	/**
	 * 安装
	 * 
	 * @param rfidNo
	 * @return
	 */
	@RequestMapping(value = "setup/{rfidNo}")
	public @ResponseBody Map<String, Object> setupRfid(
			@PathVariable String rfidNo) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("state", 1);
		map.put("contractNo", "CT1410250001");
		map.put("batchNo", "00001");
		return map;

	}
	
	/**
	 * 获取合同地址信息
	 * 
	 * @param rfidNo
	 * @return
	 * @throws SQLException 
	 * @throws ServiceException 
	 */
	@RequestMapping(value = "info/address/{contractNo}")
	public @ResponseBody Map<String, Object> getAddress(
			@PathVariable String contractNo) throws ServiceException, SQLException {
		Map<String, Object> map = new HashMap<String, Object>();
		String address =openRfidService.getAddress(contractNo);
		map.put("address", address);
		return map;
	}

}
