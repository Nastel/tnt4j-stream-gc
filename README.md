# tnt4j-stream-gc
Track and trace java GC invokations

Running Stream-GC as javaagent:
```java
java -javaagent:tnt4j-stream-gc.jar -Dtnt4j.config=tnt4j.properties -classpath "lib/tnt4j-api-final-all.jar" your.class.name your-args
```
