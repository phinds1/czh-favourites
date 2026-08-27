---
name: sanename
description: ensure grepping code can locate all code related to modules and features easily
allowed-tools: bash, read_file, write_file, sed, grep, find
---

When a module has a sanename defined, code should use exactly the sanename in functions configuration files and tests.
We should not make up abbreviations.

sanename has defined mechanisms for converting sanenames code tokens from canonical sanename-package, snake_case to UPPER_CASE etc, and for how to concatenate 
prefixes and suffixes to sanenames.

Variable names inside functions can use abbreviations, function names and structs should all use the full sanename.

Test code should be similarly strict with sanenames: we search test code as well.

ref https://sanename.org