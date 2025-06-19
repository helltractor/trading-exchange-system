<p align="center">
    <strong>📊 Trading Exchange System</strong>
</p>
<p align="center">
    <strong>一个高性能的证券交易系统，实现单一交易对的买卖功能</strong>
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

## 🚀 功能简介

Trading Exchange System 是一个简单的证券交易系统，支持以下功能：

- 下单撮合：提供限价单和市价单的撮合功能。
- 交易撮合引擎：支持实时订单匹配。
- 行情推送：通过 WebSocket 推送实时交易数据。
- 交易记录管理：提供交易历史和订单状态查询。
- 分布式架构：基于 Spring Cloud 进行服务拆分，使用 Kafka 作为消息队列。

## 🧱 项目模块

| 模块名            | 描述                         |
| ----------------- | ---------------------------- |
| config            | 配置中心，管理所有微服务配置 |
| trading-engine    | 撮合引擎，负责订单匹配       |
| trading-sequencer | 交易流水管理，保证顺序执行   |
| trading-api       | 提供 API 接口，供客户端调用  |
| quotation         | 行情数据处理                 |
| push              | 消息推送服务                 |
| ui                | 前端 UI 服务，展示交易界面   |

## ⚙️ 构建项目

在项目根目录下依次执行：

```bash
cd ./build              # 进入构建目录
docker-compose up -d    # 启动依赖服务
mvn -B package          # 编译打包项目
```

构建完成后即可启动各模块运行系统。

## 🚀 项目启动

先后启动以下服务：

```bash
java -jar ConfigApplication.jar
java -jar TradingEngineApplication.jar
java -jar TradingSequencerApplication.jar
java -jar TradingAPIApplication.jar
java -jar QuotationApplication.jar
java -jar PushApplication.jar
java -jar UIApplication.jar
```

启动完成后，访问浏览器：
👉 [http://localhost:8080](http://localhost:8080)

## 🧪 测试项目

> 更多测试细节请参考 [bot](./build/bot/README.md)
