This folder contains test scripts written in bash (see `./calude/skills/c-bash` for style guide)
It is used princiaplly for C code and snip testing ref: https://tp23.org/snip-testing.html
But can be used for locally installing and launching tests for spring-boot applications

- fuzz - tests that hit methods or services with randomized data
- integ - integration tests that load the servers and call via public apis (typically use Jamcrest asserts)
- perf - externally executed performance e.g. useing `ab` (apache bench), and snip performance tests (see /perf-testing)
- snip - functional snip tests
