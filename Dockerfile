FROM java:openjdk-8-jre
MAINTAINER Michael Hwang <hirehwang@gmail.com>

RUN wget -q -O /usr/bin/lein \
    https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
        && chmod +x /usr/bin/lein

ENV LEIN_ROOT true
ADD . /opt/jarvis-api
RUN cd /opt/jarvis-api && lein do clean, ring uberjar

ENV JARVIS_DIR_ROOT /opt/Jarvis

EXPOSE 3000

CMD ["java", "-jar", "/opt/jarvis-api/target/server.jar"]
