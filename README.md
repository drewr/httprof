# httprof

Command-line tool for replaying requests to an HTTP service.

## Get

https://download.elasticsearch.org/httprof/httprof-2.4.jar

## Usage

### Request file

Input files are single line per request.  The default parser expects `ORDTOK METHOD URI BODY`, like this:

    % wc -l enron.requests 
         500 enron.requests
    % head -1 enron.requests
    0 POST /enron/_search {"from":0,"size":10,"sort":["_score"],"query":{"query_string":{"default_field":"body","query":"type:message"}}}
    1 POST /enron/_search {"from":0,"size":10,"sort":["_score"],"query":{"query_string":{"default_field":"body","query":"type:message"}}}

### Alternate parser

This isn't enabled from the command line yet, but you can supply a
custom parser in `httprof.core/run-set` for your request format.  Look
at `httprof/parser/default.clj` for an example.

### Run

Run file through connection iterations.  Example using ElasticSearch:

    % java -jar httprof.jar enron.requests http://localhost:9200 5 10 20
    15:47:38.676 conns 5 reqs 500 secs 6.699 rate 74.638 avgrate 15.369 min 0.013 max 0.340 5%min 0.178 1xx 0 2xx 500 3xx 0 4xx 0 5xx 0
    15:47:43.219 conns 10 reqs 500 secs 4.497 rate 111.185 avgrate 11.004 min 0.012 max 0.334 5%min 0.242 1xx 0 2xx 500 3xx 0 4xx 0 5xx 0
    15:47:48.002 conns 20 reqs 500 secs 4.748 rate 105.307 avgrate 5.354 min 0.011 max 0.694 5%min 0.434 1xx 0 2xx 500 3xx 0 4xx 0 5xx 0

## License

Copyright (C) 2012 Andrew A. Raines

Distributed under the MIT license.

Thanks to [Sonian](http://www.sonian.com) for supporting open
software.
