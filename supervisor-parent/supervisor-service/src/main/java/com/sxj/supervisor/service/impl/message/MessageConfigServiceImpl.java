package com.sxj.supervisor.service.impl.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sxj.supervisor.dao.message.IMessageConfigDao;
import com.sxj.supervisor.entity.message.MessageConfigEntity;
import com.sxj.supervisor.enu.message.MessageTypeEnum;
import com.sxj.supervisor.service.message.IMessageConfigService;
import com.sxj.util.exception.ServiceException;
import com.sxj.util.logger.SxjLogger;
import com.sxj.util.message.NewSendMessage;
import com.sxj.util.persistent.QueryCondition;

@Service
@Transactional
public class MessageConfigServiceImpl implements IMessageConfigService
{
    
    @Autowired
    private IMessageConfigDao dao;
    
    @Value("${mobile.smsUrl}")
    private String smsUrl;
    
    @Value("${mobile.userName}")
    private String userName;
    
    @Value("${mobile.password}")
    private String password;
    
    @Value("${mobile.sign}")
    private String sign;
    
    @Value("${mobile.type}")
    private String type;
    
    @Override
    @Transactional
    public void addConfig(String memberNo, List<MessageConfigEntity> config)
            throws ServiceException
    {
        try
        {
            //dao.delConfig(memberNo);
            if (config != null && config.size() > 0)
            {
                dao.addConfigBatch(config);
            }
            
        }
        catch (Exception e)
        {
            SxjLogger.error("设置消息配置错误", e, this.getClass());
            throw new ServiceException("设置消息配置错误", e);
            
        }
        
    }
    
    @Override
    @Transactional
    public void updateConfig(List<MessageConfigEntity> config)
            throws ServiceException
    {
        try
        {
            for (Iterator<MessageConfigEntity> iterator = config.iterator(); iterator.hasNext();)
            {
                MessageConfigEntity messageConfigEntity = (MessageConfigEntity) iterator.next();
                dao.updateConfig(messageConfigEntity);
            }
            
        }
        catch (Exception e)
        {
            SxjLogger.error("设置消息配置错误", e, this.getClass());
            throw new ServiceException("设置消息配置错误 ", e);
        }
        
    }
    
    @Override
    @Transactional
    public List<MessageConfigEntity> queryConfigList(String memberNo)
            throws ServiceException
    {
        try
        {
            QueryCondition<MessageConfigEntity> query = new QueryCondition<>();
            query.addCondition("memberNo", memberNo);
            List<MessageConfigEntity> list = dao.queryConfigList(query);
            return list;
        }
        catch (Exception e)
        {
            SxjLogger.error("获取消息配置错误", e, this.getClass());
            throw new ServiceException("获取消息配置错误", e);
        }
    }
    
    @Override
    public MessageConfigEntity getConfig(String memberNo,
            MessageTypeEnum messageType) throws ServiceException
    {
        try
        {
            List<MessageConfigEntity> list = queryConfigList(memberNo);
            MessageConfigEntity messageConfig = null;
            if (list != null && list.size() > 0)
            {
                for (MessageConfigEntity messageConfigEntity : list)
                {
                    if (messageConfigEntity == null)
                    {
                        continue;
                    }
                    if (messageConfigEntity.getMessageType()
                            .equals(messageType))
                    {
                        if (messageConfigEntity.getIsAccetp())
                        {
                            messageConfig = messageConfigEntity;
                        }
                    }
                }
            }
            return messageConfig;
        }
        catch (Exception e)
        {
            SxjLogger.error("获取消息配置错误", e, this.getClass());
            throw new ServiceException("获取消息配置错误", e);
        }
    }
    
    @Override
    public void sendMessage(String memberNo, MessageTypeEnum messageType,
            String message) throws ServiceException
    {
        try
        {
            MessageConfigEntity config = getConfig(memberNo, messageType);
            if (config != null)
            {
                if (config.getIsAccetp())
                {
                    NewSendMessage.getInstance(smsUrl,
                            userName,
                            password,
                            sign,
                            type).sendMessage(config.getPhone(),
                            message + "，请登录私享家绿色门窗平台查看详情！");
                }
                
            }
        }
        catch (Exception e)
        {
            SxjLogger.error("发送消息短信错误", e, this.getClass());
            throw new ServiceException("发送消息短信错误", e);
        }
        
    }
    
    @Override
    public void sendAllMessage(String message) throws ServiceException
    {
        try
        {
            QueryCondition<MessageConfigEntity> query = new QueryCondition<>();
            query.addCondition("messageType", MessageTypeEnum.TENDER.getId());
            List<MessageConfigEntity> configList = dao.queryConfigList(query);
            List<String> phones = new ArrayList<String>();
            if (configList != null && configList.size() > 0)
            {
                for (Iterator<MessageConfigEntity> iterator = configList.iterator(); iterator.hasNext();)
                {
                    MessageConfigEntity config = iterator.next();
                    if (config.getIsAccetp())
                    {
                        phones.add(config.getPhone());
                    }
                    
                }
                NewSendMessage.getInstance(smsUrl,
                        userName,
                        password,
                        sign,
                        type)
                        .sendMessage(phones.toArray(new String[phones.size()]),
                                message + "，请登录私享家绿色门窗平台查看详情！");
            }
        }
        catch (Exception e)
        {
            SxjLogger.error("发送消息短信错误", e, this.getClass());
            throw new ServiceException("发送消息短信错误", e);
        }
        
    }
}
