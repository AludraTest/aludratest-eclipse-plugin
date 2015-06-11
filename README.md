# aludratest-eclipse-plugin

AludraTest Visual Data Editor Plug-In for Eclipse.

## Build Status
[![Build Status](https://travis-ci.org/AludraTest/aludratest-eclipse-plugin.svg?branch=master)](https://travis-ci.org/AludraTest/aludratest-eclipse-plugin)

## Description

This Plug-In enables Developers and Test Designers to edit Test Data XML files introduced with AludraTest 3.0.0. It consists of a Visual Data Editor to edit Test Data metadata as well as contents, and Refactoring Participants updating XML files when e.g. field names of Data classes or Data class names change.

Currently, the Plug-In is in a Beta stage and contains many TODOs. It is already usable and in use. But expect issues and unexpected behaviour when using. Please report issues using the GitHub issue tracker.

## Installation

To install the Plug-In, startup Eclipse, open Help -> Install new Software, and enter the AludraTest Plug-In Update Site:

`http://aludratest.github.io/aludratest-eclipse-plugin/updatesite`

Select the AludraTest Visual Data Editor Feature, and step through the installation. Restart Eclipse when prompted to do so.

## Build

To build the Plug-In by yourself, first of all install the Plug-In into your local Maven Repository:

`mvn clean install`

Afterwards, cd into the "feature" subdirectory, and install it as well:

`mvn clean install`

Finally, cd into the "updatesite" subdirectory, and package this:

`mvn clean package`

Now you can use the `target/repository` subdirectory of the `updatesite` directory as a local update site for Eclipse.

## Develop

To modify the Plug-In, import it as an "Existing Project" into your eclipse workspace. Create an "Eclipse Application" Run Configuration and add all required Plug-Ins. Hit "Run" to directly run your copy of the Plug-In code in a new eclipse instance.
