# Contributing

If you are on this page it means you are almost ready to contribute proposing changes, fixing issues
or anything else about the Gar Cli plugin. So, thanks a lot for this!! 🎉👍

### Prerequisites

* Skills with kotlin
* Experience with the Gradle framework
* A bookmark to
  the [official gcloud auth cli documentation](https://cloud.google.com/sdk/gcloud/reference/auth)

### Plugin design

The plugin uses this flow:

* Search the `gcloud` executable path otherwise fails
* Search for the logged user
* If user is missing or if user's token is expired (or invalid)
    * If the current machine is a CI machine 👉 FAILS
    * If not 👉 executes `gcloud auth login` and resume the next step
* If user is registered and token is valid
    * 👉 Uses the token to authenticate to the remote GAR

### Runtime environment

The plugin is compatible with Linux/MacOS and Windows, this mean system specific commands
like `which` for linux must be also translated to run upon the windows eco-system. To make it
possible, always run commands by using the extension
method `ExecSpec.commandLineMultiplatform(vararg args: Any)` and ask gradle for the current OS by
calling `DefaultNativePlatform.getCurrentOperatingSystem()`. The returns object contains methods
like `isWindows: Boolean`, `isMacOsX: Boolean` and `isLinux: Boolean`.

### Gradle Value Sources

This plugin mainly works with the `ValueSource` interface to fetch values from the current OS or
from the CLI. Some type like `AuthenticateUser` implements `ValueSource` without returning any
values. Most of the values sources uses `ExecOperations.exec {}` because most of them needs to
run CLI commands. Because Gradle is written in JAVA, some types don't handle well Kotlin features,
for instance, using `Provider<T>` with a null value is confusing, forcing you to declare a provider
like this `Provider<Optional<T>>`. To get around this boilerplate, kotlin oriented extensions were
added in the file `Tools.kt`.

### Tests

You can run test on your local machine or in your own CI, both are compatible. This repo uses a
custom Decathlon runner to run CI builds, when forking this project, you have to adapt the workflow
a little bit to make is compatible with `ubuntu-latest`. When test are run, Google Cloud
Authentication authentication is required BUT the integrity of the token is not evaluated when
connecting to the remote GAR. This choice was motivated by the need to keep private GAR urls
privates and by the fact than check the integrity of the token is something outside of the scope of
this plugin. The purpose of this plugin is to bind the local gcloud cli to gradle's declared
repositories from your project. The content itself of the authentication data it up to the Google
Cloud admin from your organisation.

When running on a CI machine the file `no-permission-service-account.json` is used as a service
account. When running on your local machine, `gcloud auth login` will be called so your web browser
will open a page and it will ask you to authenticate. Some tests requires you to log-in
successfully, some not, check the log and follow the instructions.
