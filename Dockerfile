FROM openjdk:8-jdk-alpine
LABEL maintainer="National Institute of Standards and Technology"

COPY VERSION /

ENV DEBIAN_FRONTEND noninteractive
ARG EXEC_DIR="/opt/executables"
ARG DATA_DIR="/data"

#Create folders
RUN mkdir -p ${EXEC_DIR} \
    && mkdir -p ${DATA_DIR}/inputs \
    && mkdir ${DATA_DIR}/outputs

#copy executable
COPY target/annot2mask.jar ${EXEC_DIR}/.
COPY target/annot2maskParam.xml ${EXEC_DIR}/.
COPY target/annot2maskLaunch.sh ${EXEC_DIR}/.


# Copy wipp-annotations2masks-plugin JAR
COPY target/WIPP-annotation2mask-plugin*.jar ${EXEC_DIR}/wipp-annotation2mask-plugin.jar

#Set working dir
WORKDIR ${EXEC_DIR}

# Default command. Additional arguments are provided through the command line
ENTRYPOINT ["java","-jar","wipp-annotation2mask-plugin.jar"]

