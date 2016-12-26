package cc.coodex.concrete.core.token.sharedcache;

import cc.coodex.concrete.common.Account;
import cc.coodex.concrete.common.AccountFactory;
import cc.coodex.concrete.common.BeanProviderFacade;
import cc.coodex.concrete.common.Token;
import cc.coodex.sharedcache.SharedCacheClient;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by davidoff shen on 2016-11-23.
 */
public class SharedCacheToken implements Token {

    private static final String PREFIX = SharedCacheToken.class.getCanonicalName();

    static class Data implements Serializable {
        long created = System.currentTimeMillis();
        boolean valid = true;
        Serializable currentAccountId = null;
        boolean accountCredible = false;
        HashMap<String, Serializable> map = new HashMap<String, Serializable>();

        @Override
        public String toString() {
            return "Data{" +
                    "created=" + created +
                    ", valid=" + valid +
                    ", currentAccountId=" + currentAccountId +
                    ", accountCredible=" + accountCredible +
                    ", map=" + map +
                    '}';
        }
    }

    private Data tokenData;
    private SharedCacheClient client;
    private String tokenId;
    private long maxIdleTime;

    private String cacheKey;

    SharedCacheToken(SharedCacheClient client, String tokenId, long maxIdleTime) {
        this.client = client;
        this.tokenId = tokenId;
        this.maxIdleTime = maxIdleTime;
        this.cacheKey = PREFIX + "." + this.tokenId;
        init();
    }


    private void write() {
        client.put(cacheKey, tokenData, maxIdleTime);
    }

    private synchronized void init() {
        if (tokenData == null) {
            tokenData = client.get(cacheKey);
            if (tokenData == null) {
                tokenData = new Data();
            }
        }
        write();
    }

    @Override
    public long created() {
        return tokenData.created;
    }

    @Override
    public boolean isValid() {
        return tokenData.valid;
    }

    @Override
    public void invalidate() {
        tokenData.valid = false;
        tokenData.map.clear();
        client.remove(cacheKey);
    }

    @Override
    public void onInvalidate() {

    }

    @Override
    @SuppressWarnings("unchecked")
    public <ID extends Serializable> Account<ID> currentAccount() {
        return tokenData.currentAccountId == null ? null :
                BeanProviderFacade.getBeanProvider().getBean(AccountFactory.class).getAccountByID((ID) tokenData.currentAccountId);
    }

    private boolean sameAccount(Account account) {
        if (tokenData.currentAccountId == null && account == null) return true;
        if (tokenData.currentAccountId == null || account == null) return false;
        return tokenData.currentAccountId.equals(account.getId());

    }

    @Override
    public void setAccount(Account account) {
        if (!sameAccount(account)) {
            tokenData.currentAccountId = account == null ? null : account.getId();
            write();
        }
    }

    @Override
    public boolean isAccountCredible() {
        return tokenData.currentAccountId == null ? false : tokenData.accountCredible;
    }

    @Override
    public void setAccountCredible(boolean credible) {
        if (tokenData.accountCredible != credible) {
            tokenData.accountCredible = credible;
            write();
        }
    }

    @Override
    public String getTokenId() {
        return tokenId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) tokenData.map.get(key);
    }

    @Override
    public void setAttribute(String key, Serializable attribute) {
        if (isValid()) {
            tokenData.map.put(key, attribute);
            write();
        }
    }

    @Override
    public void removeAttribute(String key) {
        if (isValid()) {
            tokenData.map.remove(key);
            write();
        }
    }

    @Override
    public Enumeration<String> attributeNames() {
        return new Vector<String>(tokenData.map.keySet()).elements();
    }

    @Override
    public void flush() {
        write();
    }

    @Override
    public String toString() {
        return "SharedCacheToken{" +
                "tokenData=" + tokenData +
                ", client=" + client +
                ", tokenId='" + tokenId + '\'' +
                ", maxIdleTime=" + maxIdleTime +
                ", cacheKey='" + cacheKey + '\'' +
                '}';
    }
}