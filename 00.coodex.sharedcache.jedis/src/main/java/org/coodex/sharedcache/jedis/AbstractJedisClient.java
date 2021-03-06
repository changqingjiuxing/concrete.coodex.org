package org.coodex.sharedcache.jedis;

import org.coodex.sharedcache.SharedCacheClient;

import java.io.*;

/**
 * Created by davidoff shen on 2016-11-23.
 */
public abstract class AbstractJedisClient implements SharedCacheClient {

    protected abstract JedisAdaptor getCommand();

//    protected abstract void closeCommand(JedisAdaptor commands);

    private long default_max_cache_time;

    public AbstractJedisClient(long default_max_cache_time) {
        this.default_max_cache_time = default_max_cache_time;
    }

    protected void assertKey(String key) {
        if (key == null) throw new NullPointerException("cache key is null.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T get(String key) {
        assertKey(key);
        JedisAdaptor commands = getCommand();
        try {
            byte[] buff = commands.get(key.getBytes());
            if (buff == null) return null;

            ByteArrayInputStream bis = new ByteArrayInputStream(buff);
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(bis);
                try {
                    return (T) ois.readObject();
                } finally {
                    ois.close();
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } finally {
            commands.close();
        }
    }

    @Override
    public void put(String key, Serializable value) {
        put(key, value, default_max_cache_time);
    }

    @Override
    public void put(String key, Serializable value, long max_cached_time) {
        assertKey(key);
        if (value == null || max_cached_time <= 0) {
            remove(key);
            return;
        }

        JedisAdaptor commands = getCommand();
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            try {
                oos.writeObject(value);
                byte[] keyBuf = key.getBytes();
                commands.set(keyBuf, bos.toByteArray());
                if (max_cached_time > 0) {
                    commands.pexpire(keyBuf, max_cached_time);
                }
            } finally {
                oos.close();
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            commands.close();
        }
    }

    @Override
    public void remove(String key) {
        if (key == null) return;
        JedisAdaptor commands = getCommand();
        try {
            commands.del(key.getBytes());
        } finally {
            commands.close();
        }
    }
}
