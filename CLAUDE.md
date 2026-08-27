DO NOT use git commands, the human user always reviews code before it is send anywhere.

Compile with Java 25 in /opt/jdk-25/
Build with `./mvn.sh {targets}` , this sets the correct environment for Javb a 25

e.g.

```
./mvn.sh install
```

All code changes need test coverage while working.
Ideally write tests first then the code. Write code and test immediatly.
Use the fasted, closest test to the code being written, for JAva code use the local JUnit test while writing code and the validate with integration tests.

Bug fixes should start with a test that replicates the bug, then fix then validate with the test.
