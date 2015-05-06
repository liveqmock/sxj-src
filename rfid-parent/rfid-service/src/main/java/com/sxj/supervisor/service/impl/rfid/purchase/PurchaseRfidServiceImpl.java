package com.sxj.supervisor.service.impl.rfid.purchase;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.sxj.supervisor.dao.rfid.purchase.IRfidPurchaseDao;
import com.sxj.supervisor.entity.rfid.apply.RfidApplicationEntity;
import com.sxj.supervisor.entity.rfid.logistics.LogisticsRfidEntity;
import com.sxj.supervisor.entity.rfid.purchase.RfidPurchaseEntity;
import com.sxj.supervisor.entity.rfid.sale.RfidPriceEntity;
import com.sxj.supervisor.entity.rfid.sale.RfidSaleStatisticalEntity;
import com.sxj.supervisor.entity.rfid.window.WindowRfidEntity;
import com.sxj.supervisor.enu.rfid.RfidStateEnum;
import com.sxj.supervisor.enu.rfid.RfidTypeEnum;
import com.sxj.supervisor.enu.rfid.apply.ReceiptStateEnum;
import com.sxj.supervisor.enu.rfid.logistics.LabelStateEnum;
import com.sxj.supervisor.enu.rfid.purchase.DeliveryStateEnum;
import com.sxj.supervisor.enu.rfid.purchase.ImportStateEnum;
import com.sxj.supervisor.enu.rfid.purchase.PayStateEnum;
import com.sxj.supervisor.enu.rfid.window.LabelProgressEnum;
import com.sxj.supervisor.model.rfid.logistics.LogisticsRfidQuery;
import com.sxj.supervisor.model.rfid.purchase.PurchaseRfidQuery;
import com.sxj.supervisor.model.rfid.window.WindowRfidQuery;
import com.sxj.supervisor.service.rfid.IRfidKeyService;
import com.sxj.supervisor.service.rfid.app.IRfidApplicationService;
import com.sxj.supervisor.service.rfid.logistics.ILogisticsRfidService;
import com.sxj.supervisor.service.rfid.purchase.IPurchaseRfidService;
import com.sxj.supervisor.service.rfid.sale.IRfidPriceService;
import com.sxj.supervisor.service.rfid.sale.IRfidSaleStatisticalService;
import com.sxj.supervisor.service.rfid.window.IWindowRfidService;
import com.sxj.util.common.DateTimeUtils;
import com.sxj.util.common.StringUtils;
import com.sxj.util.exception.ServiceException;
import com.sxj.util.logger.SxjLogger;
import com.sxj.util.persistent.CustomDecimal;
import com.sxj.util.persistent.QueryCondition;

@Service
@Transactional
public class PurchaseRfidServiceImpl implements IPurchaseRfidService
{
    
    @Autowired
    private IRfidPurchaseDao rfidPurchaseDao;
    
    @Autowired
    private IRfidSaleStatisticalService saleStatisticalService;
    
    @Autowired
    private IRfidPriceService rfidPriceService;
    
    @Autowired
    private IRfidApplicationService applyService;
    
    @Autowired
    private IWindowRfidService winRfidService;
    
    @Autowired
    private ILogisticsRfidService logisticsRfidService;
    
    @Autowired
    private IRfidKeyService keyService;
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public List<RfidPurchaseEntity> queryPurchase(PurchaseRfidQuery query)
            throws ServiceException
    {
        try
        {
            List<RfidPurchaseEntity> rfidList = new ArrayList<RfidPurchaseEntity>();
            if (query == null)
            {
                return rfidList;
            }
            QueryCondition<RfidPurchaseEntity> condition = new QueryCondition<RfidPurchaseEntity>();
            condition.addCondition("purchaseNo", query.getPurchaseNo());
            condition.addCondition("applyNo", query.getApplyNo());
            condition.addCondition("contractNo", query.getContractNo());
            condition.addCondition("supplierName", query.getSupplierName());
            condition.addCondition("rfidType", query.getRfidType());
            condition.addCondition("payState", query.getPayState());
            condition.addCondition("receiptState", query.getReceiptState());
            condition.addCondition("importState", query.getImportState());
            condition.addCondition("startDate", query.getStartDate());
            condition.addCondition("endDate", query.getEndDate());
            condition.setPage(query);
            rfidList = rfidPurchaseDao.queryList(condition);
            query.setPage(condition);
            return rfidList;
        }
        catch (Exception e)
        {
            throw new ServiceException("查询采购单错误", e);
        }
    }
    
    @Override
    @Transactional
    public void updatePurchase(RfidPurchaseEntity purchase)
            throws ServiceException
    {
        try
        {
            Assert.notNull(purchase, "采购单不存在");
            RfidPurchaseEntity old = getRfidPurchase(purchase.getId());
            Assert.notNull(old, "采购单不存在");
            if (old.getPayState().equals(PayStateEnum.PAYED))
            {
                throw new ServiceException("采购单已付款，不能修改");
            }
            if (!old.getReceiptState().equals(DeliveryStateEnum.UN_FILLED))
            {
                throw new ServiceException("采购单已发货，不能修改");
            }
            if (old.getImportState().equals(ImportStateEnum.IMPORTED))
            {
                throw new ServiceException("采购单已导入，不能修改");
            }
            RfidApplicationEntity app = applyService.getApplication(old.getApplyNo());
            Assert.notNull(app, "申请单不存在");
            app.setHasNumber((app.getHasNumber() - old.getCount())
                    + purchase.getCount());
            rfidPurchaseDao.updateRfidPurchase(purchase);
            applyService.updateApp(app);
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("更新采购单错误", e);
        }
    }
    
    @Override
    @Transactional
    public void updatePayState(String id, PayStateEnum state)
            throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = new RfidPurchaseEntity();
            purchase.setId(id);
            purchase.setPayState(state);
            rfidPurchaseDao.updateRfidPurchase(purchase);
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("更新采购单状态错误", e);
        }
        
    }
    
    @Override
    @Transactional
    public void addPurchase(RfidPurchaseEntity purchase, String applyId,
            String hasNumber) throws ServiceException
    {
        try
        {
            RfidApplicationEntity app = applyService.getApplicationInfo(applyId);
            app.setHasNumber(Long.valueOf(hasNumber) + purchase.getCount());
            purchase.setApplyNo(app.getApplyNo());
            purchase.setDateNo("CG" + DateTimeUtils.getTime("yyMM"));
            rfidPurchaseDao.addRfidPurchase(purchase);
            applyService.updateApp(app);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("新增采购单错误", e);
        }
        
    }
    
    @Override
    @Transactional(readOnly = true)
    public RfidPurchaseEntity getRfidPurchase(String id)
            throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = rfidPurchaseDao.getRfidPurchase(id);
            return purchase;
        }
        catch (Exception e)
        {
            throw new ServiceException("获取采购单错误", e);
        }
        
    }
    
    /**
     * 确认发货
     */
    @Override
    public void confirmDelivery(String id) throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = rfidPurchaseDao.getRfidPurchase(id);
            purchase.setReceiptState(DeliveryStateEnum.SHIPPED);
            rfidPurchaseDao.updateRfidPurchase(purchase);
            // 修改RFID状态
            if (purchase.getRfidType().equals(RfidTypeEnum.DOOR))
            {
                WindowRfidQuery query = new WindowRfidQuery();
                query.setPurchaseNo(purchase.getPurchaseNo());
                List<WindowRfidEntity> listRfid = winRfidService.queryWindowRfid(query);
                Assert.notNull(listRfid, "未导入RFID标签，不能发货！");
                if (listRfid.size() < purchase.getCount())
                {
                    throw new ServiceException("未完全导入RFID标签，不能发货！");
                }
                //                for (WindowRfidEntity windowRfid : listRfid)
                //                {
                //                    if (windowRfid == null)
                //                    {
                //                        continue;
                //                    }
                //                    windowRfid.setProgressState(LabelProgressEnum.SHIPPED);
                //                }
                WindowRfidEntity windowRfid = new WindowRfidEntity();
                windowRfid.setPurchaseNo(purchase.getPurchaseNo());
                windowRfid.setProgressState(LabelProgressEnum.SHIPPED);
                winRfidService.updateProgressState(windowRfid);
            }
            else
            {
                LogisticsRfidQuery query = new LogisticsRfidQuery();
                query.setPurchaseNo(purchase.getPurchaseNo());
                List<LogisticsRfidEntity> listRfid = logisticsRfidService.queryLogistics(query);
                Assert.notNull(listRfid, "未导入RFID标签，不能发货！");
                if (listRfid.size() < purchase.getCount())
                {
                    throw new ServiceException("未完全导入RFID标签，不能发货！");
                }
                //                for (LogisticsRfidEntity rfidEntity : listRfid)
                //                {
                //                    if (rfidEntity == null)
                //                    {
                //                        continue;
                //                    }
                //                    rfidEntity.setProgressState(LabelStateEnum.SHIPPED);
                //                }
                LogisticsRfidEntity rfidEntity = new LogisticsRfidEntity();
                rfidEntity.setPurchaseNo(purchase.getPurchaseNo());
                rfidEntity.setProgressState(LabelStateEnum.SHIPPED);
                logisticsRfidService.updateProgressState(rfidEntity);
            }
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("确认发货失败", e);
        }
        
    }
    
    /**
     * 确认收货
     */
    @Override
    @Transactional
    public void confirmReceipt(String id) throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = rfidPurchaseDao.getRfidPurchase(id);
            purchase.setReceiptState(DeliveryStateEnum.RECEIVING);
            rfidPurchaseDao.updateRfidPurchase(purchase);
            // 修改RFID状态
            if (purchase.getRfidType().equals(RfidTypeEnum.DOOR))
            {
                WindowRfidQuery query = new WindowRfidQuery();
                query.setPurchaseNo(purchase.getPurchaseNo());
                List<WindowRfidEntity> listRfid = winRfidService.queryWindowRfid(query);
                Assert.notNull(listRfid, "未导入RFID标签，不能收货！");
                if (listRfid.size() < purchase.getCount())
                {
                    throw new ServiceException("未完全导入RFID标签，不能收货！");
                }
                //                for (WindowRfidEntity windowRfid : listRfid)
                //                {
                //                    if (windowRfid == null)
                //                    {
                //                        continue;
                //                    }
                //                    windowRfid.setProgressState(LabelProgressEnum.HAS_RECEIPT);
                //                }
                WindowRfidEntity windowRfid = new WindowRfidEntity();
                windowRfid.setPurchaseNo(purchase.getPurchaseNo());
                windowRfid.setProgressState(LabelProgressEnum.HAS_RECEIPT);
                winRfidService.updateProgressState(windowRfid);
            }
            else
            {
                LogisticsRfidQuery query = new LogisticsRfidQuery();
                query.setPurchaseNo(purchase.getPurchaseNo());
                List<LogisticsRfidEntity> listRfid = logisticsRfidService.queryLogistics(query);
                Assert.notNull(listRfid, "未导入RFID标签，不能收货！");
                if (listRfid.size() < purchase.getCount())
                {
                    throw new ServiceException("未完全导入RFID标签，不能收货！");
                }
                //                for (LogisticsRfidEntity rfidEntity : listRfid)
                //                {
                //                    if (rfidEntity == null)
                //                    {
                //                        continue;
                //                    }
                //                    rfidEntity.setProgressState(LabelStateEnum.HAS_RECEIPT);
                //                }
                //                logisticsRfidService.batchUpdateLogistics(listRfid.toArray(new LogisticsRfidEntity[listRfid.size()]));
                LogisticsRfidEntity rfidEntity = new LogisticsRfidEntity();
                rfidEntity.setPurchaseNo(purchase.getPurchaseNo());
                rfidEntity.setProgressState(LabelStateEnum.HAS_RECEIPT);
                logisticsRfidService.updateProgressState(rfidEntity);
            }
            
            // 修改申请单状态
            String appNo = purchase.getApplyNo();
            RfidApplicationEntity apply = applyService.getApplication(appNo);
            if (apply != null)
            {
                PurchaseRfidQuery query = new PurchaseRfidQuery();
                query.setApplyNo(appNo);
                query.setReceiptState(DeliveryStateEnum.RECEIVING.getId());
                Long hasReceCount = new Long(0);
                List<RfidPurchaseEntity> list = queryPurchase(query);
                if (list != null)
                {
                    for (RfidPurchaseEntity rePurchase : list)
                    {
                        if (rePurchase == null)
                        {
                            continue;
                        }
                        hasReceCount = hasReceCount + rePurchase.getCount();
                    }
                }
                if (apply.getCount().intValue() == hasReceCount.intValue())
                {
                    apply.setReceiptState(ReceiptStateEnum.GOODS_RECRIPT);
                    applyService.updateApp(apply);
                }
            }
            // 插入销售记录
            RfidSaleStatisticalEntity entity = new RfidSaleStatisticalEntity();
            entity.setApplyNo(purchase.getApplyNo());
            entity.setPurchaseNo(purchase.getPurchaseNo());
            entity.setSaleDate(new Date());
            entity.setCount(purchase.getCount());
            entity.setRfidType(purchase.getRfidType());
            List<RfidPriceEntity> list = rfidPriceService.queryPrice();
            if (list != null && !list.isEmpty())
            {
                RfidPriceEntity price = list.get(0);
                if (purchase.getRfidType().getId() == 0)
                {
                    entity.setPrice(price.getWindowPrice());
                }
                else
                {
                    entity.setPrice(price.getLogisticsPrice());
                }
                
            }
            else
            {
                throw new ServiceException();
            }
            saleStatisticalService.add(entity);
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("确认收货失败", e);
        }
    }
    
    @Override
    @Transactional(timeout = 30)
    public void importRfid(String purchaseId) throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = getRfidPurchase(purchaseId);
            RfidApplicationEntity apply = applyService.getApplication(purchase.getApplyNo());
            RfidTypeEnum rfidType = purchase.getRfidType();
            Long count = purchase.getCount();
            Long lastkey = keyService.getKey(count.intValue());
            if (RfidTypeEnum.DOOR.equals(rfidType))
            {
                StringBuilder sb = new StringBuilder();
                for (Long i = lastkey; i > lastkey - count; i--)
                {
                    WindowRfidEntity rfid = new WindowRfidEntity();
                    rfid.setId(StringUtils.getUUID());
                    rfid.setApplyNo(purchase.getApplyNo());
                    rfid.setPurchaseNo(purchase.getPurchaseNo());
                    rfid.setContractNo(purchase.getContractNo());
                    rfid.setImportDate(new Date());
                    rfid.setMemberNo(apply.getMemberNo());
                    rfid.setMemberName(apply.getMemberName());
                    rfid.setRfidState(RfidStateEnum.UN_USED);
                    if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.UN_FILLED))
                    {
                        rfid.setProgressState(LabelProgressEnum.UN_FILLED);
                    }
                    else if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.SHIPPED))
                    {
                        rfid.setProgressState(LabelProgressEnum.SHIPPED);
                    }
                    else if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.RECEIVING))
                    {
                        rfid.setProgressState(LabelProgressEnum.HAS_RECEIPT);
                    }
                    rfid.setGenerateKey(i);
                    String rfidNo = CustomDecimal.getDecimalString(4,
                            new BigDecimal(i));
                    rfid.setRfidNo(rfidNo);
                    sb.append(rfid.toString());
                    sb.append(System.getProperty("line.separator"));
                }
                Connection connection = DataSourceUtils.getConnection(dataSource);
                PreparedStatement prepareStatement = connection.prepareStatement("LOAD DATA LOCAL INFILE 'tmp.csv' IGNORE INTO TABLE R_WINDOW_RFID fields terminated by '|' (ID,RFID_NO,GENERATE_KEY,MEMBER_NAME,MEMBER_NO,APPLY_NO,PURCHASE_NO,CONTRACT_NO,WINDOW_TYPE,GLASS_RFID,PROFILE_RFID,IMPORT_DATE,REPLENISH_NO,RFID_STATE,LOG,PROGRESS_STATE,GID) ");
                if (prepareStatement.isWrapperFor(com.mysql.jdbc.PreparedStatement.class))
                {
                    com.mysql.jdbc.PreparedStatement unwrap = prepareStatement.unwrap(com.mysql.jdbc.PreparedStatement.class);
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            sb.toString().getBytes("UTF-8"));
                    unwrap.setLocalInfileInputStream(bis);
                    int rows = unwrap.executeUpdate();
                    System.out.println(rows);
                }
                
            }
            else
            {
                StringBuffer sb = new StringBuffer();
                for (Long i = lastkey; i > lastkey - count; i--)
                {
                    LogisticsRfidEntity rfid = new LogisticsRfidEntity();
                    rfid.setId(StringUtils.getUUID());
                    rfid.setApplyNo(purchase.getApplyNo());
                    rfid.setPurchaseNo(purchase.getPurchaseNo());
                    rfid.setImportDate(new Date());
                    rfid.setRfidState(RfidStateEnum.UN_USED);
                    rfid.setMemberNo(apply.getMemberNo());
                    rfid.setMemberName(apply.getMemberName());
                    if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.UN_FILLED))
                    {
                        rfid.setProgressState(LabelStateEnum.UN_FILLED);
                    }
                    else if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.SHIPPED))
                    {
                        rfid.setProgressState(LabelStateEnum.SHIPPED);
                    }
                    else if (purchase.getReceiptState()
                            .equals(DeliveryStateEnum.RECEIVING))
                    {
                        rfid.setProgressState(LabelStateEnum.HAS_RECEIPT);
                    }
                    rfid.setType(rfidType);
                    rfid.setGenerateKey(i);
                    String rfidNo = CustomDecimal.getDecimalString(4,
                            new BigDecimal(i));
                    rfid.setRfidNo(rfidNo);
                    sb.append(rfid.toString());
                    sb.append(System.getProperty("line.separator"));
                }
                
                Connection connection = DataSourceUtils.getConnection(dataSource);
                PreparedStatement prepareStatement = connection.prepareStatement("LOAD DATA LOCAL INFILE 'tmp.csv' IGNORE INTO TABLE R_LOGISTICS_RFID fields terminated by '|' (ID,RFID_NO,GENERATE_KEY,MEMBER_NAME,MEMBER_NO,APPLY_NO,PURCHASE_NO,CONTRACT_NO,TYPE,IMPORT_DATE,BATCH_NO,IS_LOSS_BATCH,REPLENISH_NO,RFID_STATE,LOG,PROGRESS_STATE,GID) ");
                if (prepareStatement.isWrapperFor(com.mysql.jdbc.PreparedStatement.class))
                {
                    com.mysql.jdbc.PreparedStatement unwrap = prepareStatement.unwrap(com.mysql.jdbc.PreparedStatement.class);
                    ByteArrayInputStream bis = new ByteArrayInputStream(
                            sb.toString().getBytes("UTF-8"));
                    unwrap.setLocalInfileInputStream(bis);
                    unwrap.executeUpdate();
                }
            }
            purchase.setImportState(ImportStateEnum.IMPORTED);
            rfidPurchaseDao.updateRfidPurchase(purchase);
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage(), e);
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("导入RFID错误", e);
        }
        
    }
    
    @Override
    @Transactional
    public void deletePurchase(String id) throws ServiceException
    {
        try
        {
            RfidPurchaseEntity purchase = rfidPurchaseDao.getRfidPurchase(id);
            Assert.notNull(purchase, "采购单不存在");
            if (purchase.isDelstate())
            {
                throw new ServiceException("采购单已经被删除");
            }
            if (purchase.getImportState().equals(ImportStateEnum.IMPORTED))
            {
                throw new ServiceException("采购单已经导入RFID，不能被删除");
            }
            if (purchase.getPayState().equals(PayStateEnum.PAYED))
            {
                throw new ServiceException("采购单已经付款，不能被删除");
            }
            if (purchase.getReceiptState().equals(DeliveryStateEnum.SHIPPED))
            {
                throw new ServiceException("采购单已发货，不能被删除");
            }
            if (purchase.getReceiptState().equals(DeliveryStateEnum.RECEIVING))
            {
                throw new ServiceException("采购单已收货，不能被删除");
            }
            RfidApplicationEntity apply = applyService.getApplication(purchase.getApplyNo());
            if (apply != null)
            {
                apply.setHasNumber(apply.getHasNumber() - purchase.getCount());
                applyService.updateApp(apply);
            }
            purchase.setDelstate(true);
            rfidPurchaseDao.updateRfidPurchase(purchase);
        }
        catch (ServiceException e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException(e.getMessage());
        }
        catch (Exception e)
        {
            SxjLogger.error(e.getMessage(), e, this.getClass());
            throw new ServiceException("删除采购单错误！");
        }
        
    }
    
}
