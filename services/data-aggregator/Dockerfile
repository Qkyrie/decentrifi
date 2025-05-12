FROM golang:1.24-alpine AS build

WORKDIR /app

COPY go.mod go.sum ./
COPY services/data-aggregator ./services/data-aggregator/

RUN go build -o /app/data-aggregator ./services/data-aggregator

FROM alpine:3.19

WORKDIR /app

COPY --from=build /app/data-aggregator /app/data-aggregator

ENTRYPOINT ["/app/data-aggregator"]