package com.sxj.supervisor.manage.controller.rfid.purchase;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sxj.supervisor.entity.member.MemberEntity;
import com.sxj.supervisor.entity.rfid.apply.RfidApplicationEntity;
import com.sxj.supervisor.entity.rfid.base.RfidSupplierEntity;
import com.sxj.supervisor.entity.rfid.purchase.RfidPurchaseEntity;
import com.sxj.supervisor.enu.rfid.RfidTypeEnum;
import com.sxj.supervisor.enu.rfid.purchase.DeliveryStateEnum;
import com.sxj.supervisor.enu.rfid.purchase.ImportStateEnum;
import com.sxj.supervisor.enu.rfid.purchase.PayStateEnum;
import com.sxj.supervisor.manage.controller.BaseController;
import com.sxj.supervisor.model.rfid.purchase.PurchaseRfidQuery;
import com.sxj.supervisor.service.member.IMemberService;
import com.sxj.supervisor.service.rfid.app.IRfidApplicationService;
import com.sxj.supervisor.service.rfid.base.IRfidSupplierService;
import com.sxj.supervisor.service.rfid.purchase.IPurchaseRfidService;
import com.sxj.util.exception.WebException;
import com.sxj.util.logger.SxjLogger;

@Controller
@RequestMapping("/rfid/purchase")
public class PurchaseRfidController extends BaseController {
	@Autowired
	private IPurchaseRfidService purchaseRfidService;

	@Autowired
	private IRfidApplicationService appService;

	@Autowired
	private IMemberService memberService;

	@Autowired
	private IRfidSupplierService supplierService;

	/**
	 * 查询列表
	 * 
	 * @param query
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("query")
	public String queryPurchaseRfid(PurchaseRfidQuery query, ModelMap model)
			throws WebException {
		try {
			query.setPagable(true);
			List<RfidPurchaseEntity> list = purchaseRfidService
					.queryPurchase(query);
			DeliveryStateEnum[] delivery = DeliveryStateEnum.values();
			ImportStateEnum[] importState = ImportStateEnum.values();
			PayStateEnum[] payState = PayStateEnum.values();
			RfidTypeEnum[] type = RfidTypeEnum.values();
			model.put("delivery", delivery);
			model.put("importState", importState);
			model.put("payState", payState);
			model.put("type", type);
			model.put("query", query);
			model.put("list", list);
			return "manage/rfid/purchase/purchase-list";
		} catch (Exception e) {
			SxjLogger.error("查询采购单错误", e, this.getClass());
			throw new WebException("查询采购单错误");
		}
	}

	/**
	 * 删除
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("delete")
	public @ResponseBody Map<String, String> delete(String id, ModelMap model)
			throws WebException {
		Map<String, String> map = new HashMap<String, String>();
		try {
			purchaseRfidService.deletePurchase(id);
			map.put("isOK", "ok");
		} catch (Exception e) {
			SxjLogger.error("删除采购单错误", e, this.getClass());
			map.put("error", e.getMessage());
		}
		return map;
	}

	/**
	 * 确认付款
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("confirmPay")
	public @ResponseBody Map<String, String> confirmPay(String id,
			ModelMap model) throws WebException {
		try {
			RfidPurchaseEntity purchase = new RfidPurchaseEntity();
			purchase.setId(id);
			purchase.setPayState(PayStateEnum.paid);
			purchaseRfidService.updatePurchase(purchase);
			Map<String, String> map = new HashMap<String, String>();
			map.put("isOK", "ok");
			return map;
		} catch (Exception e) {
			SxjLogger.error("确认付款错误", e, this.getClass());
			throw new WebException("确认付款错误");
		}
	}

	/**
	 * 确认收货
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("confirmReceipt")
	public @ResponseBody Map<String, String> confirmReceipt(String id,
			ModelMap model) throws WebException {
		Map<String, String> map = new HashMap<String, String>();
		try {
			purchaseRfidService.confirmReceipt(id);
			map.put("isOK", "ok");
		} catch (Exception e) {
			SxjLogger.error("确认收货错误", e, this.getClass());
			map.put("error", e.getMessage());
		}
		return map;
	}

	/**
	 * 确认发货
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("confirmDelivery")
	public @ResponseBody Map<String, String> confirmDelivery(String id,
			ModelMap model) throws WebException {
		try {
			RfidPurchaseEntity purchase = new RfidPurchaseEntity();
			purchase.setId(id);
			purchase.setReceiptState(DeliveryStateEnum.shipped);
			purchaseRfidService.updatePurchase(purchase);
			Map<String, String> map = new HashMap<String, String>();
			map.put("isOK", "ok");
			return map;
		} catch (Exception e) {
			SxjLogger.error("确认发货错误", e, this.getClass());
			throw new WebException("确认发货错误");
		}
	}

	/**
	 * 申请单详情
	 * 
	 * @param id
	 * @param applyNo
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("purchaseInfo")
	public String getPurchase(String id, String applyNo, ModelMap model)
			throws WebException {
		try {
			RfidApplicationEntity app = appService.getApplication(applyNo);
			model.put("app", app);
			if (app != null) {
				MemberEntity member = memberService.memberInfo(app
						.getMemberNo());
				model.put("member", member);
			}
			model.put("id", id);
			return "manage/rfid/purchase/purchase-info";
		} catch (Exception e) {
			SxjLogger.error("查询采购单错误", e, this.getClass());
			throw new WebException("查询采购单错误");
		}
	}

	/**
	 * 修改采购单
	 * 
	 * @param purchase
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("modify")
	public @ResponseBody Map<String, String> modify(
			RfidPurchaseEntity purchase, ModelMap model) throws WebException {
		Map<String, String> map = new HashMap<String, String>();
		try {
			purchaseRfidService.updatePurchase(purchase);
			map.put("isOK", "ok");
		} catch (Exception e) {
			SxjLogger.error("修改采购单错误", e, this.getClass());
			map.put("error", e.getMessage());
		}
		return map;
	}

	/**
	 * 跳转添加
	 * 
	 * @param id
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("toAdd")
	public String toAdd(String id, ModelMap model) throws WebException {
		try {
			RfidApplicationEntity app = appService.getApplicationInfo(id);
			model.put("app", app);
			return "manage/rfid/purchase/purchase-add";
		} catch (Exception e) {
			SxjLogger.error("查询采购单错误", e, this.getClass());
			throw new WebException("查询采购单错误");
		}
	}

	/**
	 * 添加
	 * 
	 * @param purchase
	 * @param model
	 * @return
	 * @throws WebException
	 */
	@RequestMapping("add")
	public @ResponseBody Map<String, String> add(RfidPurchaseEntity purchase,
			String applyId, String hasNumber, ModelMap model)
			throws WebException {
		try {
			purchase.setPurchaseDate(new Date());
			purchase.setImportState(ImportStateEnum.not_imported);
			purchase.setPayState(PayStateEnum.unpaid);
			purchase.setReceiptState(DeliveryStateEnum.unfilled);
			purchaseRfidService.addPurchase(purchase, applyId, hasNumber);
			Map<String, String> map = new HashMap<String, String>();
			map.put("isOK", "ok");
			return map;
		} catch (Exception e) {
			SxjLogger.error("新增采购单错误", e, this.getClass());
			throw new WebException("新增采购单错误");
		}
	}

	@RequestMapping("getSupplierPrice")
	public @ResponseBody Map<String, Object> getSupplierPrice(String supplierNo)
			throws WebException {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			RfidSupplierEntity supplier = supplierService
					.getSupplierByNo(supplierNo);
			if (supplier != null) {
				Double doorPrice = supplier.getDoorsPrice();
				Double batchPrice = supplier.getBatchPrice();
				map.put("doorPrice", doorPrice);
				map.put("batchPrice", batchPrice);
			} else {
				map.put("error", "供应商不存在");
			}
		} catch (Exception e) {
			SxjLogger.error("获取价格错误", e, this.getClass());
			map.put("error", e.getMessage());
		}
		return map;
	}

	@RequestMapping("importRfid")
	public @ResponseBody Map<String, String> importRfid(String purchaseId,
			ModelMap model) throws WebException {
		try {
			purchaseRfidService.importRfid(purchaseId);
			Map<String, String> map = new HashMap<String, String>();
			map.put("isOK", "ok");
			return map;
		} catch (Exception e) {
			SxjLogger.error("导入RFID错误", e, this.getClass());
			throw new WebException("导入RFID错误");
		}
	}

}
