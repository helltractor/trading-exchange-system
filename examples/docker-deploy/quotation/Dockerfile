FROM openjdk:17
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD quotation.jar quotation.jar
EXPOSE 8005
ENTRYPOINT ["java", "-jar"]
CMD ["quotation.jar"]