FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD push.jar push.jar
EXPOSE 8006
ENTRYPOINT ["java", "-jar"]
CMD ["push.jar"]