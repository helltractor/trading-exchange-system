FROM openjdk:21
ENV APP_HOME=/apps
WORKDIR $APP_HOME
ADD trading-sequencer.jar trading-sequencer.jar
EXPOSE 8002
ENTRYPOINT ["java", "-jar"]
CMD ["trading-sequencer.jar"]