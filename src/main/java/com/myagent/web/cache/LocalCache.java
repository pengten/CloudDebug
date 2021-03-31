package com.myagent.web.cache;

import java.util.concurrent.ConcurrentHashMap;

public class LocalCache<T> {

    private ConcurrentHashMap<String, T> cacheMap = new ConcurrentHashMap<>();

    public void put(String key, T value) {
        cacheMap.put(key, value);
    }

    public T get(String key) {
        return cacheMap.get(key);
    }

}
