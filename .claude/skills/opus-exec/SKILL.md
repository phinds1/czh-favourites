---
name: opus-exec
description: executing a plan written by opus-plan
allowed-tools: java, bash
---

Claude Opus writes phased delivery plans in `.planning/plans/XXX_PLAN.md`.

Claude Sonnet should execute.  If the agent asked to execute is Opus it should stop and report this as an error.

Arguments to this skill should be the plan file path and optionally the starting phase.
The plan may also be provided as an attached document to the chat.

## File naming convention

- Plans:    `.planning/plans/XXX_PLAN.md`
- Progress: `.planning/state/XXX_PROGRESS.md`  (written by geanyprogress automatically)

## Start of execution

The plan was registered with the Geany sidebar by `opus-plan` when it was written.
Do not re-run `~/bin/geany-progress init` — that would replace the active plan.

## During execution

Execute phases autonomously, one after the other, until the context window is 80% full.

If running in geany... (If `$GEANY_PROGRESS_SOCK` is not set or the socket does not exist, skip silently)
After completing each phase, run the `~/bin/geany-progress done` command from the plan, which
includes review files and warnings specific to that phase:

```sh
geany-progress done N [-r path[:line]]... [-w "warning"]...
```

The plugin writes `.planning/state/XXX_PROGRESS.md` automatically after each call.
If not running in geany, the agent should write this file

If the context window reaches 80%, report the last completed phase number and wait for
the human to `/clear` and continue with `/opus-exec` from the next phase.
After completing a phase that creates new file agent should run (exactly) `sudo /usr/bin/updatedb`

## Constraints

Never submit code.
Never change `.perf.targets` — humans must review performance regressions.
