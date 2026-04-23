package com.boardinggate.auth.store;

/**
 * 加密材料存储（单次消费 + TTL）。
 * <p>
 * 当前默认实现为进程内 {@link InMemoryCryptoMaterialStore}；
 * 后续如需分布式或多实例部署，可替换为 Redis 实现，接口保持不变。
 */
public interface CryptoMaterialStore {

    /** 保存一次性密钥材料。 */
    void put(CryptoMaterial material);

    /**
     * 取出并作废（<b>单次消费</b>）：
     * 返回材料后必须立即失效，即使调用方之后失败也不允许再次使用。
     *
     * @return 对应材料；若不存在或已过期/已消费，返回 null
     */
    CryptoMaterial consume(String cryptoId);
}
