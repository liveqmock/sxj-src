package com.sxj.supervisor.website.login;

import java.util.List;

import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.sxj.redis.advance.RedisCollections;
import com.sxj.redis.advance.core.MessageListener;
import com.sxj.redis.advance.core.RMap;
import com.sxj.redis.advance.core.RTopic;
import com.sxj.redis.advance.topic.RedisTopics;
import com.sxj.spring.modules.security.shiro.ShiroRedisCacheManager;
import com.sxj.util.Constraints;

public class ShiroEventListener implements BeanFactoryPostProcessor {

	private static RedisTopics topics;

	private static RedisCollections collections;

	private static ShiroRedisCacheManager cacheManager;

	@Override
	public void postProcessBeanFactory(
			final ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		topics = beanFactory.getBean(RedisTopics.class);
		collections = beanFactory.getBean(RedisCollections.class);
		cacheManager = beanFactory
				.getBean(SupervisorShiroRedisCacheManager.class);
		RTopic<Object> topic = topics
				.getTopic(Constraints.WEBSITE_CHANNEL_NAME);
		topic.addListener(new MessageListener<Object>() {
			@Override
			public void onMessage(Object msg) {
				if (!(msg instanceof String)) {
					return;
				}
				String[] message = ((String) msg).split(",");
				String type = message[0];
				String accountId = message[1];
				RMap<Object, Object> map = collections
						.getMap(Constraints.SHIRO_MAP_KEY);
				if (map == null) {
					return;
				}
				if (map.get(accountId) == null) {
					return;
				}
				List<PrincipalCollection> principals = (List<PrincipalCollection>) map
						.get(accountId);
				Cache<Object, Object> cache = cacheManager
						.getCache(Constraints.WEBSITE_CACHE_NAME);
				for (PrincipalCollection principal : principals) {
					if ("del".equals(type)) {
						cache.put(principal, new SimpleAuthorizationInfo());
					} else if ("update".equals(type)) {
						SimpleAuthorizationInfo old = (SimpleAuthorizationInfo) cache
								.get(principal);
						if (old == null) {
							continue;
						}
						old.setStringPermissions(null);
						cache.put(principal, old);
					}

				}

			}
		});

	}
}
