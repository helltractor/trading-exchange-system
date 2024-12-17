FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD config.jar config.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar"]
CMD ["config.jar"]