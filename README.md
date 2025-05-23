<p align="center">
	<strong>📊 Trading Exchange System</strong>
</p>
<p align="center">
	<strong>一个简单的证券交易系统，实现单一交易对的买卖功能</strong>
</p>
<p align="center">
    <a target="blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src="https://img.shields.io/github/stars/Helltractor/trading-exchange-system.svg?style=social" alt="GitHub Stars"/>
    </a>
    <a target="_blank" href="https://opensource.org/licenses/MIT">
        <img src="https://img.shields.io/:license-GPL-blue.svg" alt="License"/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src="https://img.shields.io/badge/JDK-1.8.0_40+-green.svg" alt="JDK Version"/>
    </a>
</p>
<p align="center">
    <a target="blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Maven-3.9.6-blue.svg' alt='Maven'/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg' alt='Spring Boot'/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Spring%20Cloud-2023.0.0-green.svg' alt='Spring Cloud'/>
    </a>
</p>

---

## 🚀 **功能简介**

Trading Exchange System 是一个简单的证券交易系统，支持以下功能：

- **下单撮合**：提供限价单和市价单的撮合功能。
- **交易撮合引擎**：支持实时订单匹配。
- **行情推送**：通过 WebSocket 推送实时交易数据。
- **交易记录管理**：提供交易历史和订单状态查询。
- **分布式架构**：基于 Spring Cloud 进行服务拆分，使用 Kafka 作为消息队列。

---

## 🧱 **项目模块**

| 模块名                          | 描述                         |
| ------------------------------- | ---------------------------- |
| **ConfigApplication**           | 配置中心，管理所有微服务配置 |
| **TradingEngineApplication**    | 撮合引擎，负责订单匹配       |
| **TradingSequencerApplication** | 交易流水管理，保证顺序执行   |
| **TradingAPIApplication**       | 提供 API 接口，供客户端调用  |
| **QuotationApplication**        | 行情数据处理与推送           |
| **PushApplication**             | 消息推送服务                 |
| **UIApplication**               | 前端 UI 服务，展示交易界面   |

---

## ⚙️ **如何构建项目**

1. **进入 build 目录**

```bash
cd ./build
```

2. **启动 Docker 服务**

```bash
docker-compose up -d
```

3. **编译打包**

```bash
mvn clean package
```

---

## 🚦 **如何运行项目**

按以下顺序启动服务：

1. 启动 **配置中心**

```bash
java -jar ConfigApplication.jar
```

2. 启动 **交易引擎**

```bash
java -jar TradingEngineApplication.jar
```

3. 启动 **交易流水服务**

```bash
java -jar TradingSequencerApplication.jar
```

4. 启动 **API 网关服务**

```bash
java -jar TradingAPIApplication.jar
```

5. 启动 **行情推送服务**

```bash
java -jar QuotationApplication.jar
```

6. 启动 **消息推送服务**

```bash
java -jar PushApplication.jar
```

7. 启动 **前端 UI 服务**

```bash
java -jar UIApplication.jar
```

8. **访问系统**  
   在浏览器中打开：

```bash
http://localhost:8080
```

---

## 🧪 **测试项目**

> 更多测试细节请参考 [bot](./build/bot/README.md)

---

## 📚 **参考链接**

### **Kafka**

- [Kafka 官方文档](https://github.com/bitnami/containers/blob/main/bitnami/kafka/README.md)

### **OkHttp**

- [OkHttp 使用指南](https://square.github.io/okhttp/)
- [OkHttp GitHub 仓库](https://github.com/square/okhttp)
- [OkHttp3 使用详解 - 博客](https://www.cnblogs.com/liyutian/p/9473747.html)

### **Vert.x**

- [Vert.x 官网](https://vertx.io/)
- [Vert.x 中文文档](https://vertx-china.github.io/)
- [Vert.x GitHub 仓库](https://github.com/vert-x3)
