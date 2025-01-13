FROM openjdk:17
ENV APP_HOME=/apps
WORKDIR $APP_HOME
COPY gateway.jar gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar"]
CMD ["gateway.jar"]