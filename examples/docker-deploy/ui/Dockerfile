FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD ui.jar ui.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar"]
CMD ["ui.jar"]