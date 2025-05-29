package org.snomed.aag.data.client;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.ihtsdo.sso.integration.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
public class RVFClientFactory {

    private final Cache<String, RVFClient> clientCache;

    public RVFClientFactory() {
        this.clientCache = CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build();
    }

    public RVFClient getClient() {
        RVFClient client = null;
        String authenticationToken = SecurityUtil.getAuthenticationToken();
        if (StringUtils.hasLength(authenticationToken)) {
            client = this.clientCache.getIfPresent(authenticationToken);
        }
        if (client == null) {
            synchronized (this.clientCache) {
                authenticationToken = SecurityUtil.getAuthenticationToken();
                if (StringUtils.hasLength(authenticationToken)) {
                    client = this.clientCache.getIfPresent(authenticationToken);
                }
                if (client == null) {
                    client = new RVFClient(authenticationToken);
                    authenticationToken = SecurityUtil.getAuthenticationToken();
                    this.clientCache.put(authenticationToken, client);
                }
            }
        }

        return client;
    }
}

