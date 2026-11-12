package org.king.smartquiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 智能出题与批改系统 启动类。
 * 组件扫描根包 org.king.smartquiz，覆盖 common / framework / system 各模块。
 */
@SpringBootApplication
public class SmartQuizApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartQuizApplication.class, args);
        System.out.println("""

                ----------------------------------------------------------
                  智能出题与批改系统启动成功！
                  接口地址: http://localhost:8080/api/test/hello
                ----------------------------------------------------------
                """);
    }
}
