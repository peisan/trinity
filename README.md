# Trinity

[![Build Status](https://travis-ci.org/sisioh/trinity.png?branch=develop)](https://travis-ci.org/sisioh/trinity)

Trinity is a lightweight MVC framework based on Finagle, written in Scala.

## Concepts
- Provide MVC functions not supported by finagle.
- Support Domain Driven-Design by non CoC(Convention over Configuration).

## Features
- You can define Actions in Controller as Finagle Services.
  - URL routings are mapped to action methods, like Scalatra.
  - Otherwise, the Routing information can be aggregated outside of the controller, like Play2.
- You can use Template Engine (such as Scalatra) with Trinity.

## Functions
### Supported Functions
- Routing request to action
  - Action can be deinfed as async process by using `com.twitter.util.Future`.
  - Action can be also defined as sync process by using `com.twitter.util.FuturePool`.
  - Support action adaptor for process returns `scala.concurrent.Future`.
- Finagle's Request/Response Enhance
  - multi-part file upload
  - json format reponse
  - file resouce support
- Binding to Template Engine
  - [Scalate](http://scalate.fusesource.org/)
  - [Velocity](http://velocity.apache.org/)
  - [FreeMarker](http://freemarker.org/)
  - [Thymeleaf](http://www.thymeleaf.org/)
- Testing
  - Unit Testing
  - Integration Testing
- JRebel support
  - see this gist for installation. https://gist.github.com/j5ik2o/5660744

### Unsupported Functions
- Functions for Form, Validation 
- Functions for Persitence(sush as RDBMS/NoSQL)

## License
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
