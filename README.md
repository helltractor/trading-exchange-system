<p align="center">
	<strong>Trading Exchange System</strong>
</p>
<p align="center">
	<strong>证券交易系统， 实现单一交易对的买卖功能</strong>
</p>
<p align="center">
    <a target="blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src="https://img.shields.io/github/stars/Helltractor/trading-exchange-system.svg?style=social" alt="github star"/>
    </a>
    <a target="_blank" href="https://opensource.org/licenses/MIT">
        <img src="https://img.shields.io/:license-MIT-blue.svg" alt="license"/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/JDK-1.8.0_40+-green.svg' alt='jdk'/>
    </a>
<p/>
<p align="center">
    <a target="blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Maven-3.9.6-blue.svg' alt='maven'/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg' alt='spring boot'/>
    </a>
    <a target="_blank" href="https://github.com/Helltractor/trading-exchange-system">
        <img src='https://img.shields.io/badge/Spring%20Cloud-2023.0.0-green.svg' alt='spring boot'/>
    </a>
</p>

# Build application
1. `cd ./build`
2. `docker-compose up --d`
3. `mvn clean package`

# Run application
1. run `ConfigApplication.java`
2. run `TradingEngineApplication.java`
3. run `TradingSequencerApplication.java`
4. run `TradingAPIApplication.java`
5. run `QuotationApplication.java`
6. run `PushApplication.java`
7. run `UIApplication.java`
8. open `http://localhost:8080` in browser

# Test application
> Mind: More details in [bot](./build/bot/README.md)

# Reference

## Kafka

* [Kafka and Zookeeper](https://github.com/bitnami/containers/blob/main/bitnami/kafka/README.md)

## OkHttp
* [OkHttp3 使用详解](https://www.cnblogs.com/liyutian/p/9473747.html)
* [OkHttp](https://square.github.io/okhttp/)
* [GitHub](https://github.com/square/okhttp)

## Vert.x

* [Vert.x](https://vertx.io/)
* [Vert.x China](https://vertx-china.github.io/)
* [GitHub](https://github.com/vert-x3)
