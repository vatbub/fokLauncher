# Change Log

## [Unreleased](https://github.com/vatbub/fokLauncher/tree/HEAD)

[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.1.3...HEAD)

**Merged pull requests:**

- Bump commons-io from 2.7 to 2.8.0 [\#84](https://github.com/vatbub/fokLauncher/pull/84) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump metrics-core from 4.1.7 to 4.1.12.1 [\#81](https://github.com/vatbub/fokLauncher/pull/81) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump rome from 1.12.2 to 1.15.0 [\#79](https://github.com/vatbub/fokLauncher/pull/79) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump wiremock from 2.26.3 to 2.27.1 [\#77](https://github.com/vatbub/fokLauncher/pull/77) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump build-helper-maven-plugin from 3.1.0 to 3.2.0 [\#74](https://github.com/vatbub/fokLauncher/pull/74) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump commons-io from 2.6 to 2.7 [\#71](https://github.com/vatbub/fokLauncher/pull/71) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump awaitility from 4.0.2 to 4.0.3 [\#69](https://github.com/vatbub/fokLauncher/pull/69) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump metrics-core from 4.1.6 to 4.1.7 [\#67](https://github.com/vatbub/fokLauncher/pull/67) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump maven-antrun-plugin from 1.8 to 3.0.0 [\#66](https://github.com/vatbub/fokLauncher/pull/66) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump metrics-core from 4.1.5 to 4.1.6 [\#65](https://github.com/vatbub/fokLauncher/pull/65) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump build-helper-maven-plugin from 3.0.0 to 3.1.0 [\#64](https://github.com/vatbub/fokLauncher/pull/64) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump metrics-core from 4.1.2 to 4.1.5 [\#63](https://github.com/vatbub/fokLauncher/pull/63) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump wiremock from 2.26.0 to 2.26.3 [\#62](https://github.com/vatbub/fokLauncher/pull/62) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump wiremock from 2.25.1 to 2.26.0 [\#57](https://github.com/vatbub/fokLauncher/pull/57) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump parentPom from 1.0.1-SNAPSHOT to 1.0.3-SNAPSHOT [\#56](https://github.com/vatbub/fokLauncher/pull/56) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump launch4j-maven-plugin from 1.7.23 to 1.7.25 [\#55](https://github.com/vatbub/fokLauncher/pull/55) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump dropwizard-metrics-datadog from 1.1.13 to 1.1.14 [\#54](https://github.com/vatbub/fokLauncher/pull/54) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump wiremock from 2.19.0 to 2.25.1 [\#53](https://github.com/vatbub/fokLauncher/pull/53) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump metrics-core from 4.0.2 to 4.1.2 [\#52](https://github.com/vatbub/fokLauncher/pull/52) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump slf4j-nop from 1.6.0 to 1.7.30 [\#51](https://github.com/vatbub/fokLauncher/pull/51) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))
- Bump rome from 1.11.1 to 1.12.2 [\#50](https://github.com/vatbub/fokLauncher/pull/50) ([dependabot-preview[bot]](https://github.com/apps/dependabot-preview))

## [foklauncher-0.1.3](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.1.3) (2018-08-12)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.1.2...foklauncher-0.1.3)

**Implemented enhancements:**

- Useless error message for offline mode [\#49](https://github.com/vatbub/fokLauncher/issues/49)
- Change the design of the progressbar to be more like bootstrap [\#45](https://github.com/vatbub/fokLauncher/issues/45)
- Leverage apache commons-cli to parse the command line args [\#39](https://github.com/vatbub/fokLauncher/issues/39)
- Add the option for a CLI-only mode where no GUI pops up at all [\#38](https://github.com/vatbub/fokLauncher/issues/38)
- Add possibility to supply custom startup args to the autolaunch application [\#37](https://github.com/vatbub/fokLauncher/issues/37)

**Fixed bugs:**

- UnknownHostException not caught  when not connected to the internet [\#48](https://github.com/vatbub/fokLauncher/issues/48)
- Snapshot jars are not deleted [\#47](https://github.com/vatbub/fokLauncher/issues/47)
- NullPointerException when loading the version list in the context menu [\#46](https://github.com/vatbub/fokLauncher/issues/46)
- Improve the launch time of the launcher when the internet connection is poor [\#44](https://github.com/vatbub/fokLauncher/issues/44)
- Meta-Buttons not centered when user uses a custom screen scaling [\#43](https://github.com/vatbub/fokLauncher/issues/43)
- Exception when importing the very first app [\#42](https://github.com/vatbub/fokLauncher/issues/42)
- GUILanguage cannot be reset [\#41](https://github.com/vatbub/fokLauncher/issues/41)
- Exporting app info results in IOException [\#40](https://github.com/vatbub/fokLauncher/issues/40)
- Crash when GUI cannot be launched and autolaunch args are supplied [\#35](https://github.com/vatbub/fokLauncher/issues/35)

## [foklauncher-0.1.2](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.1.2) (2017-07-26)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.1.1...foklauncher-0.1.2)

**Implemented enhancements:**

- Force enabled snapshot downloads for autolaunched apps with command line parameter [\#34](https://github.com/vatbub/fokLauncher/issues/34)
- Create a "See changelog" button [\#30](https://github.com/vatbub/fokLauncher/issues/30)

**Fixed bugs:**

- Window not showing when launching the launcher through the installer shortcut [\#31](https://github.com/vatbub/fokLauncher/issues/31)

**Closed issues:**

- Remove the option from the installer to install the launcher to the AutoStart [\#33](https://github.com/vatbub/fokLauncher/issues/33)

## [foklauncher-0.1.1](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.1.1) (2016-12-11)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.1.0...foklauncher-0.1.1)

## [foklauncher-0.1.0](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.1.0) (2016-12-11)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.9...foklauncher-0.1.0)

**Fixed bugs:**

- Two apps with the exact same maven coordinates but a different classifier cause the launcher to be unable to launch any of them [\#28](https://github.com/vatbub/fokLauncher/issues/28)

## [foklauncher-0.0.9](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.9) (2016-12-03)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.8...foklauncher-0.0.9)

**Implemented enhancements:**

- Add possibility that apps can be launched using startup args [\#26](https://github.com/vatbub/fokLauncher/issues/26)
- Make download progress appear smoother [\#25](https://github.com/vatbub/fokLauncher/issues/25)

**Fixed bugs:**

- Closing the launcher does not cancel app downloads [\#24](https://github.com/vatbub/fokLauncher/issues/24)
- Call MainWindow.showErrorMessage\("some text", false\) still makes the app quit [\#23](https://github.com/vatbub/fokLauncher/issues/23)
- launchButton disappears on internet failure [\#22](https://github.com/vatbub/fokLauncher/issues/22)
- When an update is downloaded, the log displays a confusing message [\#21](https://github.com/vatbub/fokLauncher/issues/21)
- Weird launchButton when quickly changing the app selection [\#20](https://github.com/vatbub/fokLauncher/issues/20)

## [foklauncher-0.0.8](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.8) (2016-10-21)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.7...foklauncher-0.0.8)

## [foklauncher-0.0.7](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.7) (2016-10-19)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.6...foklauncher-0.0.7)

## [foklauncher-0.0.6](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.6) (2016-10-17)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.5...foklauncher-0.0.6)

**Closed issues:**

- Tell launched application about a changed GUI language [\#19](https://github.com/vatbub/fokLauncher/issues/19)

## [foklauncher-0.0.5](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.5) (2016-10-15)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.4...foklauncher-0.0.5)

**Implemented enhancements:**

- Implement a message of the day [\#17](https://github.com/vatbub/fokLauncher/issues/17)
- Add a "Show App info" button [\#16](https://github.com/vatbub/fokLauncher/issues/16)
- Option "Show launcher again when app quits" [\#5](https://github.com/vatbub/fokLauncher/issues/5)

**Fixed bugs:**

- Custom GUI language is not applied to other dialogs [\#18](https://github.com/vatbub/fokLauncher/issues/18)
- Fix Antivirus report of the WindowsExecutable [\#15](https://github.com/vatbub/fokLauncher/issues/15)

## [foklauncher-0.0.4](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.4) (2016-09-18)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.3...foklauncher-0.0.4)

## [foklauncher-0.0.3](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.3) (2016-09-09)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.2...foklauncher-0.0.3)

## [foklauncher-0.0.2](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.2) (2016-09-05)
[Full Changelog](https://github.com/vatbub/fokLauncher/compare/foklauncher-0.0.1...foklauncher-0.0.2)

**Implemented enhancements:**

- Finish translations [\#6](https://github.com/vatbub/fokLauncher/issues/6)
- Build a gui to delete selected artifact versions [\#3](https://github.com/vatbub/fokLauncher/issues/3)
- Build a gui to download and launch a specific version of artifacts [\#2](https://github.com/vatbub/fokLauncher/issues/2)
- Download artifact versions in separate folders and save reference to all downloaded versions [\#1](https://github.com/vatbub/fokLauncher/issues/1)

**Fixed bugs:**

- App would never exit in some cases [\#13](https://github.com/vatbub/fokLauncher/issues/13)
- If the jar of the app is deleted, no error message is displayed [\#10](https://github.com/vatbub/fokLauncher/issues/10)
- Infinite "Getting version info" when changing snapshot setting and no app is selected [\#9](https://github.com/vatbub/fokLauncher/issues/9)
- Open app first time and enable snapshots, launcher wants to "Update & launch" instead of "Download & launch" [\#8](https://github.com/vatbub/fokLauncher/issues/8)
- Infinite "Getting version info" on first launch with snapshots enabled [\#7](https://github.com/vatbub/fokLauncher/issues/7)
- Bug on first launch [\#4](https://github.com/vatbub/fokLauncher/issues/4)

## [foklauncher-0.0.1](https://github.com/vatbub/fokLauncher/tree/foklauncher-0.0.1) (2016-08-05)


\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*