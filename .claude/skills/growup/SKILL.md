---
name: growup
description: clean up noisy AI-generated comment formatting in C/H source files using bin/professionalize.sh
allowed-tools: bash
---

# professionalize

`bin/professionalize.sh` cleans up noisy formatting that AI tools commonly inject into C source files.

Run it on any `.c` or `.h` file after AI-generated code has been written or merged.

## Usage

```bash
bin/professionalize.sh src/nginx_mods/ngx_foo_module/ngx_foo.c
bin/professionalize.sh src/nginx_mods/ngx_foo_module/*.h
bin/professionalize.sh src/**/*.c src/**/*.h
```

Do **not** run it on third-party or vendored files (e.g. `src/nginx/src/`).

When called without further instructions professionalize code written in the current session. 
