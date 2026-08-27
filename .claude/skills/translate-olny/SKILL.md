---
name: translate-only
description: This skill instructs LLM to focus on translation from one code format to another rather than hallucinating or relying on natural language input.
---

<objective>
100% accurate code, cannot be achieved if the input is natural language: natural language leaves too much up to interpreteations.
</objective>

<process>
When asked to write code, the agent should first verify that a specification exists in one or more computer languages, it does not matter which languages.
The typical language pairs are the working code base and a test suite.  If the code works (existing in production), it is a perfect reference so we can change the test suite freely to match the code or to add coverage.
If the test suite works we can refactor the code, we cannot refactor code without tests as a technical spec.

Ideally we should have multiple specifications, natural language high loevel goals, unit tests, and integration tests. This skills tates one unchanging spec is required 
for work.
If there is not a sufficient specification for work, the agent should build the missing tests cases before changing code, or should stop and ask the human user for more accurate specifications.

When planning work test gaps should b treated as spec gaps and fixed first.
</process>
