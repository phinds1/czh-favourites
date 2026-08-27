---
name: cloud-build
description: setup the build jobs for github and azuredevops
---

<objective>
Build in github where possible to get as much as possibel tested in in github actions.
Build final artifacts and release version in azuredevops, which has access to xxmaven and the deployment RPM repos and nexus.
</objective>

<process>
Move directorins from `tools/build-tmp`  to `./` and edit the files to build this projects artifacts what ever they may be.

</process>
