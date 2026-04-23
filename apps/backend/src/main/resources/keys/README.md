# JWT 密钥目录

本地联调可以 **留空**：应用启动时若找不到 `jwt-private.pem` / `jwt-public.pem`，会自动生成临时 2048 位 RSA 密钥对（每次重启都会换，仅适用于开发）。

生产环境请在本目录放入固定的密钥文件，并把它们加入 `.gitignore`，不要提交到仓库：

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-private.pem
openssl rsa -in jwt-private.pem -pubout -out jwt-public.pem
```

> 默认按 PKCS#8 私钥 + X.509 公钥解析，和 `openssl` 默认输出格式一致。
