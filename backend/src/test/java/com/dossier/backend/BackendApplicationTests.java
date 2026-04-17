package com.dossier.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 应用启动入口测试
 * 完整的 Spring Context 加载（contextLoads）需要运行中的 PostgreSQL，
 * 属于集成测试范畴，在没有数据库的 CI 环境中跳过。
 * 单元测试覆盖由各模块的 *Test 类负责。
 */
class BackendApplicationTests {

    @Test
    @DisplayName("BackendApplication 主类存在且可实例化")
    void mainClassExists() {
        // 验证主类存在，不启动 Spring Context（无需数据库）
        BackendApplication app = new BackendApplication();
        // 有类即通过，Spring Context 集成测试见 docker-compose 环境
    }
}
