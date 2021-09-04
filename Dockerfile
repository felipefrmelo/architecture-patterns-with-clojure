FROM clojure

WORKDIR /usr/src/app
COPY project.clj /usr/src/app
RUN lein deps

COPY . /usr/src/app


#MAINTAINER Your Name <you@example.com>
#
#ADD target/pedrepl-0.0.1-SNAPSHOT-standalone.jar /pedrepl/app.jar
#
#EXPOSE 8080
#
#CMD ["java", "-jar", "/pedrepl/app.jar"]
