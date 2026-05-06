## 目录结构说明

```
2025-2026Fail-SSE-SoftwareEngineeringCourseProject【EchoMark】

|--SRS —————————— SRS文档 
|--需求规约 —————— 需求规约文档
|--... —————————— 后续文档
|__programming —— 源代码根文件夹
```

### Programming目录具体结构

```
Programming/
│-- common/  —————————— 跨服务共用代码
|   └--dto/  —————————— 跨服务共用DTO类
│-- gateway/  ————————— 网关服务（用于将多个不同端口的微服务集成到同一端口）
└-- services/  ———————— 各微服务
    |-- AuthService/ —— 用户认证服务
    |-- AudioService/ —— 音频服务
    |-- SocialService/ —— 社交服务
    └-- UserService/ —— 用户服务

```

- 其中微服务目录结构以AuthService为例
  ```
  AuthService/src
   ├── main       ← 微服务业务逻辑主目录
   │   ├── java   ← 源代码目录
   │   │   └── org
   │   │       └── tongji
   │   │           └── sse   ← org.tongji.sse是包名
   │   │               ├── controller   ← REST API控制层，接收 HTTP 请求，调用 Service 并返回 DTO
   │   │               │   └── AuthController.java
   │   │               │
   │   │               ├── service      ← 业务逻辑层
   │   │               │   ├── AuthService.java
   │   │               │   └── impl
   │   │               │       └── AuthServiceImpl.java
   │   │               │
   │   │               ├── repository   ← 数据库访问层
   │   │               │   └── UserRepository.java
   │   │               │
   │   │               ├── entity       ← 数据库映射的实体类
   │   │               │   └── User.java ← 直接对应数据库中特定表的定义
   │   │               │
   │   │               ├── security     ← 存放 Spring Security + JWT 相关逻辑
   │   │               │   ├── JwtAuthenticationFilter.java
   │   │               │   ├── JwtUtil.java
   │   │               │   └── SecurityConfig.java
   │   │               │
   │   │               ├── dto          ← 前端交互的数据对象
   │   │               │   ├── UserLoginRequestDTO.java
   │   │               │   ├── UserRegisterRequestDTO.java
   │   │               │   ├── LoginResponseDTO.java
   │   │               │   ├── UserResponseDTO.java
   │   │               │   └── ApiResponse.java
   │   │               │
   │   │               │
   │   │               └── AuthServiceApplication.java  ← Springboot微服务程序启动入口
   │   │
   │   └── resources  ← 配置文件/资源文件主目录
   │       └── application.properties ← 微服务主程序配置文件
   │
   └── test      ← 微服务测试单元主目录
       ├── java
       │   └── org
       │       └── tongji
       │           └── sse
       │               └── authservicetest
       │                   ├── controller  ← 只针对Controller层数据接收解析和返回格式是否正确的单元测试
       │                   │   └── AuthControllerTest.java
       │                   └── integration
       │                       └── AuthIntegrationTest.java   ← 集成测试，全流程注册登录（使用H2内存数据库模拟，不会访
       |                                                        问真实数据库）
       │
       └── resources
           └── application-test.properties  ← 测试程序的配置文件

  ```

## pom.xml文件说明

对于一个多模块java项目，可以使用maven工具来对项目结构进行管理，而pom.xml文件用于告诉maven：各模块的父子关系，各模块使用到的第三方依赖或者本地跨模块依赖，java和第三方库版本管理，如何构建和编译各模块的代码等信息

### 1.项目整体 pom 结构说明

对于一个多模块项目：

```
root
│--Programming
│   │-- pom.xml       
│   │-- common/
│   │   └── pom.xml
│   │-- gateway/
│   │   └── pom.xml
│   └-- services/
│       └-- AuthService/
│           └── pom.xml
│
└──pom.xml

```

父 pom 管理全局依赖版本、Java 版本、Spring Boot 版本
子 pom 覆写自己的依赖

（一个 pom 可以是父模块的父模块）

### 2.父 pom 说明
父 pom 通常包含这几项：

1. groupId，artifactId，version
   指明该模块属于哪一个包，叫什么名，版本号多少
   比如org.tongji.sse，root，1.0.0
2. parent
   继承 `spring-boot-starter-parent`或者上级父模块
   → 统一依赖管理、默认插件配置。
3. packaging=pom
   说明这个模块只是父模块，不生成 jar。
4. modules
   声明所有子模块，实现 Maven 聚合构建：
   `mvn clean install`
   即可编译 common、gateway、AuthService。
5. dependencyManagement（一般只需要最顶级的父模块写就可以了）
   用于统一版本管理，使子模块不需要重复写版本号：
   * 避免多个模块版本冲突
   * 更易维护
   * Spring Cloud 推荐做法

### 3. 子 pom 说明
子 pom 通常包含这几项：

1. groupId，artifactId，version
   指明该模块属于哪一个包，叫什么名，版本号多少
   比如org.tongji.sse，services，1.0.0
2. parent
   继承上级父模块
3. packaging=jar
   说明这个模块是子模块，编译时需要生成 jar。
4. dependencies
   说明该子模块使用到哪些依赖项
    