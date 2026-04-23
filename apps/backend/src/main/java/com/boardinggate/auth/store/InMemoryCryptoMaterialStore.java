package com.boardinggate.auth.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内加密材料存储（线程安全）。
 * <p>
 * 仅在 {@code app.auth.crypto-store.type=memory} 时生效。不支持多实例部署，仅用于本地调试。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.auth.crypto-store", name = "type", havingValue = "memory")
public class InMemoryCryptoMaterialStore implements CryptoMaterialStore {

    private final ConcurrentHashMap<String, CryptoMaterial> store = new ConcurrentHashMap<>(1024);

    @Override
    public void put(CryptoMaterial material) {
        store.put(material.getCryptoId(), material);
    }

    @Override
    public CryptoMaterial consume(String cryptoId) {
        if (cryptoId == null || cryptoId.isEmpty()) {
            return null;
        }
        CryptoMaterial material = store.remove(cryptoId);
        if (material == null) {
            return null;
        }
        if (material.isExpired()) {
            return null;
        }
        return material;
    }

    /** 每 60 秒清理一次过期条目，避免长期未消费的材料堆积。 */
    @Scheduled(fixedDelay = 60_000L)
    public void evictExpired() {
        int removed = 0;
        Iterator<Map.Entry<String, CryptoMaterial>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CryptoMaterial> e = it.next();
            if (e.getValue().isExpired()) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0 && log.isDebugEnabled()) {
            log.debug("清理过期 SM4 加密材料 {} 条，剩余 {} 条", removed, store.size());
        }
    }
}
