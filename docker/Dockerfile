FROM nexus3.onap.org:10001/onap/integration-java11:8.0.0

COPY --chown=onap:onap a1-pe-simulator*.jar /a1-pe-simulator/a1-pe-simulator.jar

# Create conf directory where vnf.config and topology files will be placed by ONAP controller
USER root
RUN mkdir /var/netconf2 && chown onap:onap /var/netconf2
# Create folder for FTP upload via file system and change owner
RUN mkdir -p /a1pesim/generated_pm_bulks && chown onap:onap /a1pesim/generated_pm_bulks
USER onap

COPY --chown=onap:onap resource/* /var/netconf2/

WORKDIR /a1-pe-simulator

EXPOSE 9998

ENTRYPOINT exec java $JAVA_SEC_OPTS $JAVA_OPTS -Djava.security.egd=file:/dev/urandom -Duser.home=/a1-pe-simulator -Dproc.name=a1-pe-simulator -jar a1-pe-simulator.jar
