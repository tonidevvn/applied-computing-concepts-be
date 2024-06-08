FROM openjdk:21
VOLUME /tmp
ARG JAR_FILE=target/*.jar
ARG EXE_FILE=exe/.
COPY ${JAR_FILE} app.jar

#Copy chromedriver for Selenium
RUN mkdir -p /app/bin
COPY exe/chromedriver-linux64/. /app/bin/

# Set links
RUN echo "Cleaning and setting links" \
    && ln -s /app/bin/chromedriver \
    && chmod 777 /app/bin/chromedriver

ENTRYPOINT ["java","-jar","/app.jar"]