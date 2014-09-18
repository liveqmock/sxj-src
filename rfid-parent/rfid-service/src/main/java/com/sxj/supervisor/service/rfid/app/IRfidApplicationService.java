package com.sxj.supervisor.service.rfid.app;

import java.util.List;

import com.sxj.supervisor.entity.rfid.apply.RfidApplicationEntity;
import com.sxj.supervisor.model.rfid.app.RfidApplicationQuery;
import com.sxj.util.exception.ServiceException;

public interface IRfidApplicationService {
	/**
	 * 根据条件查询
	 * 
	 * @param query
	 * @return
	 * @throws ServiceException
	 */
	public List<RfidApplicationEntity> query(RfidApplicationQuery query)
			throws ServiceException;

	/**
	 * 更新
	 * 
	 * @param app
	 * @throws ServiceException
	 */
	public void updateApp(RfidApplicationEntity app) throws ServiceException;

	/**
	 * 逻辑删除
	 * 
	 * @param app
	 * @throws ServiceException
	 */
	public void delApp(String id) throws ServiceException;
}