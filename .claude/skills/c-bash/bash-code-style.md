# Bash code style

Use `#/bin/bash` shebang.

start with `set -euo pipefail` to make the script more robust.

`for`, `if`, `do` & `while` statements should be two lines.

```
if ...
then
   ...
fi
```

avoid `;` line termination, each statement on its own line.

Line length 140 at least
Ideally keep logging lines on one line to ease grepping source code.

Environment variables should be `$UPPER_CASE`
Other local bash variables should be lower `$snake_case`, like the C code we write.
Avoid grepping logs for assertions, its brittle.

Avoid _unnecessary_ quotes, some quites are necessary.
Quote echo strings so they syntax highlighter renders them in green.
Prefer `$var` without brackets `${var}` if they are not needed. N.B. `echo $body` does need quotes if `$body` has spaces, newlines or quotes, or unknown content.
Don't quote declarations that have no need for it, like `var=value` or `var=$(command)`

Avoid heredocs, use file in test `./fixtures` instead.
