## 📝 开发日志

**2025-08-06**
*   导入商品浏览相关代码
*   管理端添加Redis白名单机制,员工登录后token存入Redis;登出后删除;拦截器处添加判断token是否在白名单和emp账号状态的逻辑
*   开启注解方式的缓存管理SpringCache,缓存分类查询的套餐和菜品列表,修改菜品状态时两个缓存都清空

---
**2025-08-05**
*   使用HttpClient完成微信登录功能
*   完成管理端套餐接口，完善菜品停售和套餐起售的逻辑
---

**2025-08-01**
*   创建Redis配置类RedisConfig,创建RedisTemplate对象
*   完成店铺营业状态设置相关接口，使用Redis存储店铺营业状态
---
**2025-07-31**
*   完成了菜品相关接口的三层编码
*   实现文件上传通用接口，aliyun OSS 存储菜品图片
*   修改了.gitignore,取消同步dev配置文件

---
**2025-07-30**
*   导入分类相关接口的代码
*   完成公共字段填充的切面类AutoFillAspect，使用了反射和ThreadLocal

---
**2025-07-29**
*   扩展spring框架的消息转换器，JacksonObjectMapper extends ObjectMapper

---
**2025-07-27**
*   导入Spring Boot 项目
*   完成了员工相关接口的三层编码
*   通过knife4j生成接口文档
*   创建了项目的 Git 仓库

---
