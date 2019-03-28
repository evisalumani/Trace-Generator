# Trace Generator

This Java application generates JSON representations of traces based on logs from microservices. 

## Execution
The program can be packaged with Maven as jar and executed with 2 parameters for the input file for the logs and the output file for the generated traces:
```
mvn package
java -jar target/trace-generator-1.0-SNAPSHOT.jar input.txt output.txt
```

## Overview

A *trace* represents an end-to-end collection of calls starting from the initial request on the first service to all other downstream services. Whereas a *call* represents a single request from a service to another (i.e. from a parent to a child span), as part of a trace. 

The following assumptions hold:

* The log format structure: `startTimestamp endTimestamp traceId serviceName callerSpan->span`
* traceId is generated at the first service receiving an outside request and is passed further along to each downstream service call (e.g. via specific headers).
* span Ids are generated at each service, thus a parent->child relationship can be established among them.
* The initial request has no parent and its callerSpan is marked with the string "null".

As part of a monitoring application, these logs can be arranged in a JSON tree to represent the hierarchy of calls/spans of a trace. 

For example, the logs for the 2 traces below:
```
2019-03-01T08:00:09.000Z 2019-03-01T08:00:10.000Z trace1 service4 span2->span4
2019-03-01T08:00:01.000Z 2019-03-01T08:00:09.000Z trace1 service3 span1->span3
2019-03-01T08:00:05.000Z 2019-03-01T08:00:08.000Z trace1 service2 span1->span2
2019-03-01T08:00:00.000Z 2019-03-01T08:00:10.000Z trace1 service1 null->span1
2019-03-01T08:00:02.000Z 2019-03-01T08:00:04.000Z trace2 service6 span5->span6
2019-03-01T08:00:00.000Z 2019-03-01T08:00:04.000Z trace2 service5 null->span5
```
can be represented with 2 JSON trees (separated by a newline) as below:
```
{
"id": "trace1",
"root": {
  "service": "service1",
  "start": "2019-03-01T08:00:00.000Z",
  "end": "2019-03-01T08:00:10.000Z",
  "span": "span1",
  "calls": [
  	{
      "service": "service3",
      "start": "2019-03-01T08:00:01.000Z",
      "end": "2019-03-01T08:00:09.000Z",
      "span": "span3",
      "calls": []
    },
    {
      "service": "service2",
      "start": "2019-03-01T08:00:05.000Z",
      "end": "2019-03-01T08:00:08.000Z",
      "span": "span2",
      "calls": [
        {
          "service": "service4",
          "start": "2019-03-01T08:00:09.000Z",
          "end": "2019-03-01T08:00:10.000Z",
          "span": "span4",
          "calls": []
        }]
    }]
  }
}
{
"id": "trace2",
"root": {
  "service": "service5",
  "start": "2019-03-01T08:00:00.000Z",
  "end": "2019-03-01T08:00:04.000Z",
  "span": ,
  "calls": [
    {
      "service": "service6",
      "start": "2019-03-01T08:00:02.000Z",
      "end": "2019-03-01T08:00:04.000Z",
      "span": "span6",
      "calls": []
    }
  ]}
}
```