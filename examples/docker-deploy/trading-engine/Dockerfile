FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD trading-engine.jar trading-engine.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar"]
CMD ["trading-engine.jar"]