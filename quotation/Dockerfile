FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
COPY ./target/quotation.jar ./quotation.jar
EXPOSE 8005
ENTRYPOINT ["java", "-jar"]
CMD ["quotation.jar"]