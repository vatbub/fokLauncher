# fokLauncher
This is a launcher for any java application that is released to a maven repository. It is currently used by [Hangman Solver](https://github.com/vatbub/hangman-solver) and a (currently) closed source Tic Tac Toe implementation.

Though the list of projects that currently use the launcher is small, it can be used by any app that is released to a maven repository which means that you can use it for your app too!

## Get the launcher
We currently don't use GitHub releases and release instead on Bintray. [Click here to get the latest version of the launcher.](https://bintray.com/vatbub/fokprojectsReleases/foklauncher#downloads)

### Upgrading from older versions
The FOKLauncher has an integrated update checker that does the entire job for you, even if you've used the installer to install the FOKLauncher.
 
Once a new version is available, the FOKLauncher will inform you at startup. If you don't wish to upgrade right now, you can ignore the update and repeat the update at any time using the *"Check for updates"*-link.

### Download the latest snapshot
If you want to use a cutting edge copy of the launcher, you can obtain one [here](https://oss.jfrog.org/webapp/#/builds/foklauncher/).
Just click the ID of the top most build, click the Module ID, give it a second and then select the file you want to download.

**Keep in mind:**
- You don't want to download `foklauncher-[version].jar`, this file will not work. Download `foklauncher-[version]-jar-with-dependencies.jar` instead.
- You don't want to download the `pom` file either, this is just the build config file.
- The `...-sources.jar` file obviously contains the uncompiled source files

## Build your own version
### Build the current snapshot
1. Clone this repository
2. Run `mvn package`

### Build the latest release
Repeat the steps mentioned above but switch to the `release` branch by running `git checkout release` prior to running `mvn package`.

## What it does
The launcher has a App-Store like gui. It presents the user a list of available applications. The user can then choose an app from the list and the launcher will download and launch that app. 
Once the app is downloaded, it is cached on the users hard drive (that means that the launcher will download it only on the first launch) but if you publish an update, the launcher will automatically detect that and download the update.

## I want my app to be in the launcher too!
Great! To add your app to the launcher, you need to meet the prerequisites (see below) and do one of the following:

### Contact us so that we add it to the list of apps on our server
In that case, please tell us the following:
- The name of your app
- The maven groupId and artifactId (No need to tell us the version as the launcher will figure that out automatically)
- The maven classifier of the file to download (if you use one, in most cases "jar-with-dependencies")
- The url of the maven repository you use to publish releases
- The url of the maven repository you use to publish snapshots

### Prepare a `.foklauncher`-file with all information about your app
- Prepare a file that looks like this:

```
#This file stores info about a java app. To open this file, get the foklauncher
#Mon Sep 05 20:11:09 CEST 2016
name=<human readable name of your app>
groupId=<maven groupId>
repoBaseURL=<url of your maven release repo>
classifier=jar-with-dependencies
snapshotRepoBaseURL=<url of your maven snapshot repo>
artifactId=<maven artifactId>
```

Please make sure to escape all `:` in the urls (they should look like this then: `https\://oss.jfrog.org/artifactory/repo`)

- Save the file as a text-file and give it a good name, e. g. `myAwsomeApp.foklauncher`
- Make sure that the file extension is `.foklauncher`
- Prompt the user to download the launcher (You may send them over to [this url](https://bintray.com/vatbub/fokprojectsReleases/foklauncher#downloads))
- Let the user download your `.foklauncher`-file
- Tell the user to drag'n'drop the `.foklauncher`-file into the launcher window.
- Your app will be imported to the launcher and can be launched as expected.

## Prerequisites
To be able to add your app to the launcher, it needs to meet the following conditions:
- It must be released to a maven repository. Ideally, you use [maven](http://maven.apache.org/) for that, but you can use any build tool that can release software to a maven repo.
- Your app must have a public release repository and snapshot repository. There are plans to make the snapshot repository optional, see [#14](/../../issues/14) for the current progress
- The release repo and snapshot repo cannot have the same url.
- Your app must be packaged to a runnable jar file. That means, that all of your dependencies must be packaged in that jar file too and a main class must be defined in the jars manifest (See [this](http://stackoverflow.com/questions/1729054/including-dependencies-in-a-jar-with-maven) and [this](http://www.avajava.com/tutorials/lessons/how-do-i-specify-a-main-class-in-the-manifest-of-my-generated-jar-file.html) for help)
- Currently, the launcher only supports to download one jar file and no additional files. If you need to download some additional files for your app, either implement the download in your app or submit a new issue for that.

## That is too complicated for me, heeeeeeeelp!
If you have any problems, don't hesitate to write an email and we will be happy to help!

## Customize the launcher in your way
If you wish to have a completely customized launcher, you can fork the repository and modify it as you wish. Just make sure to respect the [License](../master/LICENSE.txt).
There are plans to make complete customization easier than forking, you can see the progress in [#12](/../../issues/12)

## Contributing
Contributions of any kind are very welcome. Just fork and submit a Pull Request and we will be happy to merge. Just keep in mind that we use [Issue driven development](https://github.com/vatbub/defaultRepo/wiki/Issue-driven-development).
