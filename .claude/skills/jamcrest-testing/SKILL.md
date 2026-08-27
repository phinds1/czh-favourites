---
name: jamcrest-testing
description: Create tests that hit a running spring-boot microservice and validate the JSON output is as expected
allowed-tools: java, bash
---

The way we write these tests is to create the Java code with one @Test method for each scenario we want to test.

These tests should create AbstractRestTest which handles starting the spring-boot server and mock database and provides us with Rest test features to call the server on the correct port.

Between each @Test the database is wiped clean.

Test typically use these two fixtures to mock the host system and populate the test database.

If it does not exist we should create JamcrestUtils.java similar to the example in this skill's directory.

```
```

Then we create the `.req.js` files with the JSON request we want to send, sometimes we can use the same `.req.js` for multiple tests, because jamcrest templating engine supports replacing variables, but typically we have one `.req.js` per @Test.

Then we run the tests without creating the `.resp.js` files yet.

The first time it runs (if `.resp.js` is missing) it prints the expected js in a file called `target/ai.log`.

If the test failed before getting to the response `ai.log` will not contain expected output yet.

Based on `ai.log`, we can create all the missing `.resp.js` files, verifying each time that the expected win-divisions and prizes were indeed won by our new code and new test data.

We can then re-run the tests and verify that they pass.
