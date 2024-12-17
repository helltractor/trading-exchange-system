FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD trading-api.jar trading-api.jar
EXPOSE 8001
ENTRYPOINT ["java", "-jar"]
CMD ["trading-api.jar"]