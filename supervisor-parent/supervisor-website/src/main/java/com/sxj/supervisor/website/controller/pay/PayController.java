package com.sxj.supervisor.website.controller.pay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sxj.redis.service.comet.CometServiceImpl;
import com.sxj.supervisor.entity.pay.PayRecordEntity;
import com.sxj.supervisor.enu.contract.PayStageEnum;
import com.sxj.supervisor.model.comet.MessageChannel;
import com.sxj.supervisor.model.contract.ContractModel;
import com.sxj.supervisor.model.contract.ContractPayModel;
import com.sxj.supervisor.model.login.SupervisorPrincipal;
import com.sxj.supervisor.service.contract.IContractPayService;
import com.sxj.supervisor.service.contract.IContractService;
import com.sxj.supervisor.website.controller.BaseController;
import com.sxj.util.LoginToken;
import com.sxj.util.common.EncryptUtil;
import com.sxj.util.exception.WebException;
import com.sxj.util.logger.SxjLogger;

@Controller
@RequestMapping("/pay")
public class PayController extends BaseController {
	@Autowired
	private IContractPayService payService;

	/**
	 * 合同service
	 */
	@Autowired
	private IContractService contractService;

	/**
	 * 获取付款信息列表
	 * 
	 * @param map
	 * @param query
	 * @param session
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("paylist")
	public String paylist(ModelMap map, ContractPayModel query,
			HttpServletRequest request, HttpSession session)
			throws WebException {
		try {
			if (query != null) {
				query.setPagable(true);
			}
			SupervisorPrincipal info = getLoginInfo(session);
			String memberNo = info.getMember().getMemberNo();
			query.setMemberNo(memberNo);
			query.setMemberType(info.getMember().getType().getId().toString());
			List<PayRecordEntity> list = payService.queryPayList(query);
			PayStageEnum[] payState = PayStageEnum.values();
			map.put("list", list);
			if (info.getMember().getType().getId() == 0) {
				map.put("state", "a");
			} else {
				map.put("state", "b");
			}
			map.put("payState", payState);
			map.put("query", query);
			String channelName = MessageChannel.WEBSITE_PAY_MESSAGE + memberNo;
			map.put("channelName", channelName);
			// 注册监听

			registChannel(channelName);
		} catch (Exception e) {
			throw new WebException(e.getMessage());
		}
		return "site/pay/pay";
	}

	/**
	 * 根据合同号获取详细信息
	 * 
	 * @param model
	 * @param contractNo
	 * @param recordNo
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("info")
	public String queryContractInfo(ModelMap model, String contractNo, String id)
			throws WebException {
		try {
			ContractModel contract = contractService
					.getContractModelByContractNo(contractNo);
			ContractModel contractModel = new ContractModel();
			if (contract.getContract() != null) {
				contractModel = contractService.getContract(contract
						.getContract().getId());
			}
			model.put("contractModel", contractModel);
			model.put("recordNo", id);
			if (contractModel.getContract().getType().getId() == 0) {
				return "site/record/contract-info-zhaobiao";
			} else {
				return "site/record/contract-info";
			}
		} catch (Exception e) {
			SxjLogger.error("查询合同信息错误", e, this.getClass());
			throw new WebException("查询合同信息错误");
		}
	}

	/**
	 * 甲方付款
	 */
	@RequestMapping("pay")
	public @ResponseBody Map<String, String> pay(String id, Double payReal)
			throws WebException {
		try {
			String flag = payService.pay(id, payReal);
			Map<String, String> map = new HashMap<String, String>();
			if (flag.equals("ok")) {
				PayRecordEntity pay = payService.getPayRecordEntity(id);
				// 甲方
				CometServiceImpl.subCount(MessageChannel.WEBSITE_PAY_MESSAGE
						+ pay.getMemberNo_A());
				MessageChannel.initTopic().publish(
						MessageChannel.WEBSITE_PAY_MESSAGE
								+ pay.getMemberNo_A());
				// 乙方
				CometServiceImpl.takeCount(MessageChannel.WEBSITE_PAY_MESSAGE
						+ pay.getMemberNo_B());
				MessageChannel.initTopic().publish(
						MessageChannel.WEBSITE_PAY_MESSAGE
								+ pay.getMemberNo_B());
				map.put("isOk", "ok");
			} else {
				map.put("isOk", "false");
			}
			return map;
		} catch (Exception e) {
			throw new WebException("甲方付款错误");
		}
	}

	/**
	 * 乙方确认付款
	 */
	@RequestMapping("pay_ok")
	public @ResponseBody Map<String, String> pay_ok(String id)
			throws WebException {
		try {
			String flag = payService.pay_ok(id);
			Map<String, String> map = new HashMap<String, String>();
			if (flag.equals("ok")) {
				PayRecordEntity pay = payService.getPayRecordEntity(id);
				// 乙方
				CometServiceImpl.subCount(MessageChannel.WEBSITE_PAY_MESSAGE
						+ pay.getMemberNo_B());
				MessageChannel.initTopic().publish(
						MessageChannel.WEBSITE_PAY_MESSAGE
								+ pay.getMemberNo_B());
				map.put("isOk", "ok");
			} else {
				map.put("isOk", "false");
			}
			return map;
		} catch (Exception e) {
			throw new WebException("乙方确认付款错误");
		}
	}

	/**
	 * 测试
	 */
	@RequestMapping("tofinance")
	public String tofinance(String payId, HttpSession session) {
		try {
			SupervisorPrincipal loginInfo = getLoginInfo(session);
			if (loginInfo == null) {
				return LOGIN;
			}
			LoginToken loginToken = new LoginToken();
			loginToken.setMemberNo(loginInfo.getMember().getMemberNo());
			loginToken.setMemberName(loginInfo.getMember().getName());
			loginToken.setPassword(loginInfo.getMember().getPassword());

			return "redirect:http://127.0.0.1:8080/finance-website/to_login.htm?member="
					+ loginToken.getMemberNo()
					+ "&token="
					+ EncryptUtil.md5Hex(loginToken.toString());
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}
}
