package com.sxj.supervisor.service.tasks.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sxj.cache.manager.CacheLevel;
import com.sxj.cache.manager.HierarchicalCacheManager;
import com.sxj.spring.modules.mapper.JsonMapper;
import com.sxj.supervisor.dao.gather.AlDao;
import com.sxj.supervisor.entity.gather.AlEntity;
import com.sxj.supervisor.service.tasks.IAlGather;
import com.sxj.supervisor.tasks.Model.DataMap;

@Service()
public class AlGatherImpl implements IAlGather {

	@Autowired
	private AlDao ad;

	@Override
	public void gather() {
		try {
			String name = getJsonString("");
			DataMap dm = JsonMapper.nonEmptyMapper().fromJson(name,
					DataMap.class);
			String oldDate = (String) HierarchicalCacheManager.get(
					CacheLevel.REDIS, "Al", "date");
			String newDate = "";
			boolean flag = false;
			for (Map<String, String> map : dm.getData().get("3").values()) {
				if (oldDate == null) {
					flag = true;
				}
				if (oldDate != null && map.get("date").equals(oldDate)) {
					flag = true;
					continue;
				}
				if (flag) {
					AlEntity alEntity = new AlEntity();
					alEntity.setDate(map.get("date"));
					alEntity.setMax(map.get("max"));
					alEntity.setMin(map.get("min"));
					alEntity.setAverage(map.get("average"));
					ad.addAl(alEntity);
					newDate = map.get("date");
				}
			}
			HierarchicalCacheManager.set(CacheLevel.REDIS, "Al", "date",
					newDate);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public String getJsonString(String urlPath) throws Exception {
		URL url = new URL("http://market.cnal.com/share/market/cj30.json");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection
				.setRequestProperty("User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:32.0) Gecko/20100101 Firefox/32.0");
		connection.setRequestMethod("GET");
		connection.connect();
		InputStream inputStream = connection.getInputStream();
		// 对应的字符编码转换
		Reader reader = new InputStreamReader(inputStream, "UTF-8");
		BufferedReader bufferedReader = new BufferedReader(reader);
		String str = null;
		StringBuffer sb = new StringBuffer();
		while ((str = bufferedReader.readLine()) != null) {
			sb.append(str);
		}
		reader.close();
		connection.disconnect();
		return sb.toString();
	}
}