package com.sxj.supervisor.service.impl.rfid.open;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sxj.supervisor.dao.contract.IContractBatchDao;
import com.sxj.supervisor.dao.contract.IContractDao;
import com.sxj.supervisor.dao.contract.IContractModifyBatchDao;
import com.sxj.supervisor.dao.contract.IContractReplenishBatchDao;
import com.sxj.supervisor.dao.rfid.logistics.ILogisticsRfidDao;
import com.sxj.supervisor.dao.rfid.ref.ILogisticsRefDao;
import com.sxj.supervisor.dao.rfid.window.IWindowRfidDao;
import com.sxj.supervisor.dao.rfid.windowref.IWindowRfidRefDao;
import com.sxj.supervisor.entity.contract.ContractBatchEntity;
import com.sxj.supervisor.entity.contract.ContractEntity;
import com.sxj.supervisor.entity.contract.ModifyBatchEntity;
import com.sxj.supervisor.entity.contract.ReplenishBatchEntity;
import com.sxj.supervisor.entity.pay.PayRecordEntity;
import com.sxj.supervisor.entity.rfid.logistics.LogisticsRfidEntity;
import com.sxj.supervisor.entity.rfid.ref.LogisticsRefEntity;
import com.sxj.supervisor.entity.rfid.window.WindowRfidEntity;
import com.sxj.supervisor.entity.rfid.windowref.WindowRefEntity;
import com.sxj.supervisor.enu.contract.PayContractTypeEnum;
import com.sxj.supervisor.enu.contract.PayModeEnum;
import com.sxj.supervisor.enu.contract.PayStageEnum;
import com.sxj.supervisor.enu.contract.PayTypeEnum;
import com.sxj.supervisor.enu.rfid.RfidStateEnum;
import com.sxj.supervisor.enu.rfid.logistics.LabelStateEnum;
import com.sxj.supervisor.enu.rfid.ref.AuditStateEnum;
import com.sxj.supervisor.enu.rfid.window.LabelProgressEnum;
import com.sxj.supervisor.model.contract.BatchItemModel;
import com.sxj.supervisor.model.contract.ContractModel;
import com.sxj.supervisor.model.open.Bacth;
import com.sxj.supervisor.model.open.BatchModel;
import com.sxj.supervisor.model.open.Contract;
import com.sxj.supervisor.model.open.WinTypeModel;
import com.sxj.supervisor.service.contract.IContractPayService;
import com.sxj.supervisor.service.contract.IContractService;
import com.sxj.supervisor.service.rfid.open.IOpenRfidService;
import com.sxj.supervisor.service.util.JsonMapperUtil;
import com.sxj.util.common.StringUtils;
import com.sxj.util.exception.ServiceException;
import com.sxj.util.logger.SxjLogger;
import com.sxj.util.persistent.QueryCondition;

@Service
@Transactional
public class OpenRfidServiceImpl implements IOpenRfidService {

	/**
	 * 批次DAO
	 */
	@Autowired
	private IContractBatchDao contractBatchDao;

	/**
	 * 变更合同批次DAO
	 */
	@Autowired
	private IContractModifyBatchDao contractModifyBatchDao;

	/**
	 * 补损合同批次
	 */
	@Autowired
	private IContractReplenishBatchDao contractReplenishBatchDao;

	@Autowired
	private ILogisticsRfidDao logisticsDao;

	@Autowired
	private ILogisticsRefDao logisticsRefDao;

	@Autowired
	private IWindowRfidDao windowRfidDao;

	@Autowired
	private IContractDao contractDao;

	@Autowired
	private IContractService contractService;

	@Autowired
	private IContractPayService contractPayService;

	@Autowired
	private IWindowRfidRefDao windowRfidRefDao;

	@Override
	public BatchModel getBatchByRfid(String gid) throws ServiceException,
			SQLException {
		BatchModel batchModel = new BatchModel();
		Bacth batch = new Bacth();
		try {
			String rfid = logisticsDao.getRfid(gid).get(0);
			if (StringUtils.isNotEmpty(rfid)) {
				QueryCondition<LogisticsRfidEntity> logisticsQuery = new QueryCondition<LogisticsRfidEntity>();
				logisticsQuery.addCondition("rfidNo", rfid);
				List<LogisticsRfidEntity> ref = logisticsDao
						.queryLogisticsRfidList(logisticsQuery);

				QueryCondition<LogisticsRefEntity> logisticsRefQuery = new QueryCondition<LogisticsRefEntity>();
				logisticsRefQuery.addCondition("rfidNo", rfid);
				List<LogisticsRefEntity> logisticsRef = logisticsRefDao
						.queryList(logisticsRefQuery);

				if (!CollectionUtils.isEmpty(logisticsRef)) {
					LogisticsRefEntity lRef = logisticsRef.get(0);

					if (!CollectionUtils.isEmpty(ref)) {
						LogisticsRfidEntity le = ref.get(0);
						Contract contract = new Contract();
						contract.setContractNo(le.getContractNo());
						batchModel.setContract(contract);// 封装合同号
						batchModel.setRfidState(le.getProgressState().getId());// 封装RFID状态
						contract.setRfid(rfid);
						QueryCondition<ContractBatchEntity> query = new QueryCondition<ContractBatchEntity>();
						query.addCondition("rfidNo", rfid);
						query.addCondition("state", 1);// 是否已变更
						// 合同批次
						List<ContractBatchEntity> cbatchList = contractBatchDao
								.queryBacths(query);

						QueryCondition<ModifyBatchEntity> modifyQuery = new QueryCondition<ModifyBatchEntity>();
						modifyQuery.addCondition("rfidNo", rfid);
						modifyQuery.addCondition("state", 1);// 是否已变更
						// 变更批次
						List<ModifyBatchEntity> modifyBatch = contractModifyBatchDao
								.queryBacths(modifyQuery);

						QueryCondition<ReplenishBatchEntity> replenishQuery = new QueryCondition<ReplenishBatchEntity>();
						replenishQuery.addCondition("newRfidNo", rfid);
						// 补损批次
						List<ReplenishBatchEntity> batchList = contractReplenishBatchDao
								.queryReplenishBatch(replenishQuery);

						setBatchItems(batch, cbatchList, modifyBatch, batchList);

					}
					if (lRef.getState().getId() == 0) {
						batch.setState("3");// 未审核
					}

				}
			} else {
				batch.setState("0");
			}
		} catch (ServiceException e) {
			batch.setState("0");
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			batch.setState("0");
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException("获取批次错误", e);
		}
		batchModel.setBatchList(batch);
		return batchModel;
	}

	private void setBatchItems(Bacth batch,
			List<ContractBatchEntity> cbatchList,
			List<ModifyBatchEntity> modifyBatch,
			List<ReplenishBatchEntity> batchList) {
		if (cbatchList != null && cbatchList.size() > 0) {
			ContractBatchEntity cbe = cbatchList.get(0);
			batch.setBatchNo(cbe.getBatchNo());
			List<BatchItemModel> batchModelList = JsonMapperUtil
					.getBatchItems(cbe.getBatchItems());
			batch.setBatchItems(batchModelList);
			batch.setState("1");
		} else if (modifyBatch != null && modifyBatch.size() > 0) {
			ModifyBatchEntity modiy = modifyBatch.get(0);
			batch.setBatchNo(modiy.getBatchNo());
			List<BatchItemModel> batchModelList = JsonMapperUtil
					.getBatchItems(modiy.getBatchItems());
			batch.setBatchItems(batchModelList);
			batch.setState("1");
		} else if (batchList != null && batchList.size() > 0) {
			ReplenishBatchEntity rbe = batchList.get(0);
			batch.setBatchNo(rbe.getBatchNo().toString());
			List<BatchItemModel> batchModelList = JsonMapperUtil
					.getBatchItems(rbe.getBatchItems());
			batch.setBatchItems(batchModelList);
			batch.setState("1");
		} else
			batch.setState("2");
	}

	/**
	 * 获取门窗
	 */
	@Override
	public WinTypeModel getWinTypeByRfid(String gid) throws ServiceException,
			SQLException {
		WinTypeModel wtm = new WinTypeModel();
		try {
			String rfid = logisticsDao.getRfid(gid).get(0);
			if (StringUtils.isNotEmpty(rfid)) {
				QueryCondition<WindowRfidEntity> query = new QueryCondition<WindowRfidEntity>();
				query.addCondition("rfidNo", rfid);

				List<WindowRfidEntity> win = windowRfidDao
						.queryWindowRfidList(query);
				QueryCondition<WindowRefEntity> refQuery = new QueryCondition<WindowRefEntity>();
				refQuery.addCondition("rfidNo", rfid);
				List<WindowRefEntity> winRef = windowRfidRefDao
						.queryWindowRfidRefList(refQuery);

				if (winRef != null && winRef.size() > 0) {
					WindowRefEntity windowRef = winRef.get(0);
					if (windowRef.getState().getId() == 1) {
						if (win != null && win.size() > 0) {
							WindowRfidEntity wre = win.get(0);
							wtm.setContratcNo(wre.getContractNo());
							wtm.setRfidNo(wre.getRfidNo());
							if (wre.getWindowType() != null) {
								wtm.setWinType(wre.getWindowType().getName());
							}
							wtm.setState("1");// 成功
						} else {
							wtm.setState("2");// 未启用
							wtm.setRfidNo(rfid);
						}
					} else {
						wtm.setState("3");// 未审核
						wtm.setRfidNo(rfid);
					}
				} else {
					wtm.setState("2");// 未启用
					wtm.setRfidNo(rfid);
				}

			}
			return wtm;
		} catch (ServiceException e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException("获取门窗错误", e);
		}

	}

	/**
	 * 获取门窗
	 */
	@Override
	public List<WinTypeModel> getWinTypeByRfids(String[] gid)
			throws ServiceException, SQLException {
		try {
			List<String> rfidList = logisticsDao.getRfid(gid);
			List<WinTypeModel> wtmList = new ArrayList<WinTypeModel>();
			if (rfidList != null && rfidList.size() > 0) {
				for (String rfid : rfidList) {
					WinTypeModel wtm = new WinTypeModel();
					QueryCondition<WindowRfidEntity> query = new QueryCondition<WindowRfidEntity>();
					query.addCondition("rfidNo", rfid);
					List<WindowRfidEntity> win = windowRfidDao
							.queryWindowRfidList(query);

					QueryCondition<WindowRefEntity> refQuery = new QueryCondition<WindowRefEntity>();
					refQuery.addCondition("rfidNo", rfid);
					List<WindowRefEntity> winRef = windowRfidRefDao
							.queryWindowRfidRefList(refQuery);
					if (!CollectionUtils.isEmpty(win)
							&& !CollectionUtils.isEmpty(winRef)) {
						WindowRefEntity windowRef = winRef.get(0);
						if (windowRef.getState()
								.equals(AuditStateEnum.APPROVAL)) {// 已审核
							WindowRfidEntity wre = win.get(0);
							if (wre.getProgressState().equals(
									LabelProgressEnum.INSTALL)) {
								wtm.setContratcNo(wre.getContractNo());
								wtm.setRfidNo(wre.getRfidNo());
								if (wre.getWindowType() != null) {
									wtm.setWinType(wre.getWindowType()
											.getName());
								}
								wtm.setState("1");// 成功
								wtmList.add(wtm);
							}

						}
					}

				}
			}
			return wtmList;
		} catch (ServiceException e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException("获取门窗错误", e);
		}

	}

	/**
	 * 获取合同地址
	 */
	@Override
	public String getAddress(String contractNo) throws ServiceException,
			SQLException {
		QueryCondition<ContractEntity> query = new QueryCondition<ContractEntity>();
		query.addCondition("contractType", "0");
		query.addCondition("contractNo", contractNo);
		List<ContractEntity> ceList = contractDao.queryContract(query);
		String address = "";
		if (!CollectionUtils.isEmpty(ceList)) {
			address = ceList.get(0).getEngAddress();
		}
		return address;
	}

	/**
	 * 发货
	 */
	@Override
	@Transactional
	public int shipped(String gid) throws ServiceException, SQLException,
			JsonParseException, JsonMappingException, IOException {
		try {
			String rfid = logisticsDao.getRfid(gid).get(0);
			if (StringUtils.isNotEmpty(rfid)) {
				QueryCondition<LogisticsRfidEntity> logisticsQuery = new QueryCondition<LogisticsRfidEntity>();
				logisticsQuery.addCondition("rfidNo", rfid);
				List<LogisticsRfidEntity> logistics = logisticsDao
						.queryLogisticsRfidList(logisticsQuery);
				QueryCondition<LogisticsRefEntity> logisticsRefQuery = new QueryCondition<LogisticsRefEntity>();
				logisticsRefQuery.addCondition("rfidNo", rfid);
				List<LogisticsRefEntity> logisticsRef = logisticsRefDao
						.queryList(logisticsRefQuery);// 审核状态
				if (!CollectionUtils.isEmpty(logistics)
						&& !CollectionUtils.isEmpty(logisticsRef)) {
					LogisticsRfidEntity le = logistics.get(0);
					LogisticsRefEntity lRef = logisticsRef.get(0);
					if (lRef.getState().equals(AuditStateEnum.APPROVAL)) {
						if (le.getRfidState().equals(RfidStateEnum.DISABLE)) {
							return 4; // 标签已停用
						}
						if (le.getProgressState().equals(
								LabelStateEnum.HAS_RECEIPT)) {// 标签是否收货状态
							le.setProgressState(LabelStateEnum.INSTALL);
							logisticsDao.updateLogisticsRfid(le);
							// 更新出库状态
							ContractBatchEntity contractBatch = contractBatchDao
									.getBacthsByRfid(rfid);
							if (contractBatch != null) {
								if (contractBatch.getType() == 1) {
									ContractBatchEntity cbe = new ContractBatchEntity();
									cbe.setId(contractBatch.getId());
									cbe.setWarehouseState(1);
									contractBatchDao.updateBatch(cbe);
								} else if (contractBatch.getType() == 2) {
									ModifyBatchEntity modifyBatch = new ModifyBatchEntity();
									modifyBatch.setId(contractBatch.getId());
									modifyBatch.setWarehouseState(1);
									contractModifyBatchDao
											.updateBatch(modifyBatch);
								} else if (contractBatch.getType() == 3) {
									ReplenishBatchEntity replenishBatch = new ReplenishBatchEntity();
									replenishBatch.setId(contractBatch.getId());
									replenishBatch.setWarehouseState(1);
									contractReplenishBatchDao
											.updateBatch(replenishBatch);
								}
							}
							return 1;
						} else if (le.getProgressState().equals(
								LabelStateEnum.INSTALL)) {
							return 2; // 货物已出库
						} else if (le.getProgressState().equals(
								LabelStateEnum.HAS_QUALITY)) {
							return 3; // 货物已验收
						}
					} else {
						return 5;
					}
				}
			}
		} catch (ServiceException e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException("更新批次错误", e);
		}

		return 0;
	}

	/**
	 * 验收
	 */
	@Override
	@Transactional
	public int accepting(String gid) throws ServiceException {
		try {
			String rfid = logisticsDao.getRfid(gid).get(0);
			if (StringUtils.isNotEmpty(rfid)) {
				QueryCondition<LogisticsRfidEntity> logisticsQuery = new QueryCondition<LogisticsRfidEntity>();
				logisticsQuery.addCondition("rfidNo", rfid);
				List<LogisticsRfidEntity> logistics = logisticsDao
						.queryLogisticsRfidList(logisticsQuery);
				QueryCondition<LogisticsRefEntity> logisticsRefQuery = new QueryCondition<LogisticsRefEntity>();
				logisticsRefQuery.addCondition("rfidNo", rfid);
				List<LogisticsRefEntity> logisticsRef = logisticsRefDao
						.queryList(logisticsRefQuery);// 审核状态
				if (!CollectionUtils.isEmpty(logistics)
						&& !CollectionUtils.isEmpty(logisticsRef)) {
					LogisticsRfidEntity le = logistics.get(0);
					LogisticsRefEntity lRef = logisticsRef.get(0);
					if (lRef.getState().equals(AuditStateEnum.APPROVAL)) {
						if (le.getRfidState().equals(RfidStateEnum.DISABLE)) {
							return 4; // 标签已停用
						}
						if (le.getProgressState()
								.equals(LabelStateEnum.INSTALL))// 标签是都已出库
						{
							le.setProgressState(LabelStateEnum.HAS_QUALITY);
							logisticsDao.updateLogisticsRfid(le);
							// 获取合同信息
							ContractModel cm = contractService
									.getContractModelByContractNo(le
											.getContractNo());
							if (cm != null) {
								// 获取批次信息
								ContractBatchEntity cb = contractBatchDao
										.getBacthsByRfid(rfid);
								// 生成支付单
								PayRecordEntity pay = new PayRecordEntity();
								pay.setMemberNoA(cm.getContract()
										.getMemberIdA());
								pay.setMemberNameA(cm.getContract()
										.getMemberNameA());
								pay.setMemberNoB(cm.getContract()
										.getMemberIdB());
								pay.setMemberNameB(cm.getContract()
										.getMemberNameB());
								pay.setContractNo(cm.getContract()
										.getContractNo());
								pay.setRfidNo(rfid);
								pay.setDateNo(cm.getContract().getContractNo()
										+ "P");// 编号
								String bu= "";
								if(cb.getType()==3){
									bu="补";
								}
								pay.setBatchNo(bu+cb.getBatchNo());
								pay.setPayAmount(cb.getAmount());
								if (cm.getContract().getType().getId() == 1) {
									pay.setContractType(PayContractTypeEnum.GLASS);
									pay.setContent(bu+"第" + cb.getBatchNo()
											+ "批次玻璃货款");
								} else if (cm.getContract().getType().getId() == 2) {
									pay.setContractType(PayContractTypeEnum.EXTRUDERS);
									pay.setContent(bu+"第" + cb.getBatchNo()
											+ "批次型材货款");
								}
								pay.setState(PayStageEnum.STAGE1);
								pay.setPayMode(PayModeEnum.MODE1);
								pay.setPayType(PayTypeEnum.PAYMENT);
								contractPayService.addPayRecordEntity(pay);// 生成支付单
								return 1;
							}
						} else if (le.getProgressState().equals(
								LabelStateEnum.HAS_QUALITY)) {
							return 2;// 货物已验收
						} else if (le.getProgressState().equals(
								LabelStateEnum.INSTALL)) {
							return 3; // 货物未出库
						}
					} else {
						return 5;
					}
				}
			}
			return 0;
		} catch (ServiceException e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException(e.getMessage());
		} catch (Exception e) {
			SxjLogger.error(e.getMessage(), e, this.getClass());
			throw new ServiceException("更新批次错误", e);
		}
	}
}
