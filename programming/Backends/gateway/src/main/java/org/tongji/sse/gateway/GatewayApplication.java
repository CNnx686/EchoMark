package org.tongji.sse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// 明确排除数据库自动配置，防止 Gateway 因为没有配置 Datasource 而启动报错
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  API Gateway 启动成功   ლ(´ڡ`ლ)ﾞ");
        System.out.println("Swagger UI 地址: http://localhost:8080/swagger-ui.html");
    }
}
