# Warp exchange

## Build

### docker-compose.yml

```PowerShell
docker-compose up --d
```
> WARNING: Don't specify the groupId in the `pom.xml` file, otherwise the `mvn` command will fail.

## Knowledge

### @Value

在Spring框架中，@Value注解用于注入属性值。@Value可以接受两种形式的参数：${}和#{}，它们的区别在于：

* ${}：用于获取外部配置文件（如application.properties或application.yml）中的属性值。例如，@Value("${server.port}")
  会获取配置文件中server.port的值。
* #{}：用于执行Spring表达式语言（SpEL）。SpEL是一种强大的表达式语言，支持在运行时查询和操作对象图。例如，@Value("
  #{systemProperties['java.home']}")会获取Java的安装目录。

所以，如果你只是想从配置文件中获取一个值，那么使用${}就足够了。如果你需要更复杂的操作，如调用方法、访问对象属性、计算表达式等，那么你应该使用#{}。

## Reference

* [Spring Cloud 开发](https://www.liaoxuefeng.com/wiki/1252599548343744/1266263401691296)
* [GitHub](https://github.com/michaelliao/warpexchange/)