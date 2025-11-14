## gar-credentials

This plugin replaces and simplifies the usage of Google authenticated artifact registries. This
plugin exposes a extension method named `garCliCredentials` which replaces the
method `credentials {}`when declaring a maven repository. Instead of the custom scheme used by the
official plugin `artifactregistry://`, this plugin uses the classic `https://` scheme.

This plugin was designed to be used with
the [centralized repository declaration](https://docs.gradle.org/current/userguide/dependency_management.html#sub:centralized-repository-declaration)
syntax which uses the `dependencyResolutionManagement{ }` lambda in `settings.gradle.kts` files.

This plugin was tested on windows and linux/macos.

## how-to

Apply the plugin into a `settings.gradle.kts` file like this:

```kotlin
// Put this code at the beginning of the settings.gradle.kts file
import com.decathlon.android.gradle.authenticatedgarcli.garCliCredentials

plugins {
    id("com.decathlon.gradle.authenticated-gar-cli") version "<latest-version>"
}
```

Declare a `maven` repo like this

```kotlin
dependencyResolutionManagement {
    repositories {
        maven {
            setUrl("https://europe-west1-maven.pkg.dev/wonderful-repository")
            garCliCredentials(this)
        }
    }
}
```

Then, **run a gradle sync**. If the `gcloud` cli is not installed an exception will be fired. Once
the cli is installed, the plugin will ask for the user to authenticate by
running `gcloud auth login`. This will open the web browser automatically and will ask you to
authenticate with your google account. When it's done, the gradle daemon will resume automatically
the configuration phase and continue its execution.

## Limitations

* C.I. are not supported yet, you have to
  run `gcloud auth activate-service-account --key-file=/path/to/service-account.json` before
  starting gradle or
  an [other login method](https://cloud.google.com/sdk/gcloud/reference/auth/login)
* Only works with `settings.gradle.kts` files
* `garCliCredentials` method is a Kotlin extension to be used with gradle kts, groovy code was not
  tested

## Help

Before submitting an issue, run your project with `--info` enabled like this `./gradlew --info` and
search for logs sent by the logger used in `AuthenticatedGarCliPlugin.kt`.

### 403 or 401 errors when downloading dependencies

This appends often when the active account in the `gcloud` CLI is not the right one.
Run `gcloud auth list` into your IDE's terminal and check the account you're expecting to be used is
correctly selected.

### Python errors fired containing `mach-o file, but is an incompatible architecture (have 'arm64', need 'x86_64')`

We're not sure why this issue is fired but it appends sometimes, delete the
folder `~/.config/gcloud/`, uninstall Python3 and reinstall `gcloud`

## TODO

* Releases: Add configuration to publish to the gradle plugin portal and setup to CI to publish
  automatically
* Service account support ? Useful when running this plugin on a C.I. 