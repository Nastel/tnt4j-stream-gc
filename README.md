# tnt4j-stream-gc
Track and trace java GC behavior using JAVA JMX GC notifications. 
Features:
* Measure GC performance, duration
* Measure memory pools before and after GC cycles
* Stream GC details to file, log4j
* Stream to https://www.jkoolcloud.com for visual analysis (requires JESL library)

Running Stream-GC as javaagent:
```java
java -javaagent:tnt4j-stream-gc.jar -Dtnt4j.config=tnt4j.properties -classpath "lib/tnt4j-api-final-all.jar" your.class.name your-args
```

# Project Dependencies
Stream-GC requires the following:
* JDK 1.7+ (Update 04 or higher)
* TNT4J (https://github.com/Nastel/TNT4J)
