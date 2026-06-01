FROM ubuntu:latest
LABEL authors="mayan"

ENTRYPOINT ["top", "-b"]