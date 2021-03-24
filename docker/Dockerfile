FROM openjdk:8

COPY a1-pe-simulator*.jar /a1-pe-simulator/a1-pe-simulator.jar

# Create conf directory where vnf.config and topology files will be placed by ONAP controller
RUN mkdir /var/netconf2
COPY resource/* /var/netconf2/

WORKDIR /a1-pe-simulator

EXPOSE 9998

CMD ["java", "-Djava.security.egd=file:/dev/urandom", "-Duser.home=/a1-pe-simulator", "-Dproc.name=a1-pe-simulator", "-jar", "a1-pe-simulator.jar"]
