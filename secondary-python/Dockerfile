FROM ubuntu:bionic
LABEL key="DmVol"
WORKDIR /app
RUN apt-get update
RUN apt-get install python3 -y
RUN apt-get install python3-pip -y
RUN pip3 install grpcio
RUN pip3 install grpcio-tools

ADD app /app/

EXPOSE 50051 50052