# Release Process

## Versioning

The release process enforce a specific versioning scheme.

```
MAJOR.MINOR.PATCH
```

Where:

- `MAJOR` is the major version number
- `MINOR` is the minor version number
- `PATCH` is the patch version number (only use PATCH, dont make incompatible changes)

## How to Create a Release

### Execute release process

1. Go to the GitHub Releases page for the repository.
2. Click on the **Draft a new release** button.
3. Define target branch (`develop` is default)
4. Click on **Choose a tag**.
    * Enter the version you want to release, usually current version without "-SNAPSHOT" suffix and click on **Create a new tag**.
4. Set the **Release title** to the version you want to release or leave blank, it will be automatically generated.
5. Click on **Generate release notes**.
6. Click on **Publish release**.

## Release Automation

Please make sure all checks in GitHub actions have completed sucessfully, if not, the Azure builds will not be triggered:

https://github.com/igt-LotteryServiceDelivery/czh-favourites/actions/workflows/prepare-release-action-branch.yaml

Azure DevOps link to release build:

https://dev.azure.com/IGT-Lottery/Devops%20Core%20Systems%20-%20Non-Production/_build?definitionScope=%5CMaven%5Cczh-favourites

**Maven Build Release Action Branch**

### Release steps

The **Prepare Release Action Branch** GitHub Action is run when GitHub Release is published.
The action does the following.

1. Checkout the Release tag.
2. Validate the tag matches the repository version schema.
3. Create and push a new branch named `action@release#<version>`.

In ADO following actions are run:
1. The new branch triggers an Azure DevOps pipeline that does the repository specific release process.
2. The new branch triggers second Azure DevOps pipeline that does the maven tasks like setting next snapshot version or any custom actions and generate PR to branch release was run on in GitHub.
3. PR is opened to target branch with changes to next snapshot versions.
4. Branch created by GitHub workflow is removed after release is built in ADO.
5. Review and merge PR with next snapshot version. **Make sure branch is removed after PR is merged !!!**


git pull seen the next SNAPSHOT version.
