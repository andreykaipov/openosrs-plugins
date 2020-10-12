## Setup

This project includes the [OpenOSRS
client](https://github.com/open-osrs/runelite) as a submodule. Before importing
this project into IntelliJ or wherever, make sure to initialize and update the
`client` submodule:

```shell
git submodule update --init client
```

Alternatively, just clone this repo with `--recurse-submodules` flag:

```shell
git clone --recurse-submodules https://github.com/andreykaipov/openosrs-plugins
```

Afterwards:

1. Import it as a Gradle project
1. Get the project's Gradle version - `gradle wrapper`
1. Build the client - `gradle client:build -x test`
1. Build the plugins - `gradle jar`

## FAQ

### Why include the client as a submodule?

The alternative involves cloning the client as a separate project altogther, and
publishing all of its build artifacts to the local Maven cache (`~/.m2`), so
that our plugins can declare the client modules as dependencies inside our
build scripts.

However, the above workflow feels a bit disjointed. Even though we may not need
to change the client code and publish artifacts often, it's easy to forget to do
so when we do, considering the projects are separated, potentially saving us
some troubleshooting. Further, a client submodule allows us to have versionless
client module dependencies, cleaning up the build scripts. Lastly, the project
feels more self-contained this way.

Note that including the client as a composite Gradle build does slow down the
build time of our plugins by 2 or 3 seconds, as Gradle has to make sure the
client modules are always up to date. Oh well.

TODO auto update the submodule with the latest master

### Where is the Gradle wrapper?

Committing the Gradle wrapper to our VCS is the largest scam since the hot
singles in my area wanting to talk with me.

The wrapper is for anybody that doesn't already have Gradle available on their
system. That's very unlikely, especially since any capable IDE provides a Gradle
plugin. If we're too stubborn for IDEs, we should use a package manager to
install Gradle instead.

But wait - what if my Gradle version is different than the one needed for this
project? The Gradle version is specified by the `wrapper` task of the root
project. We can just run `gradle wrapper` ourselves to fetch the right version
for this project. However, it's likely our IDE's Gradle plugin is already
configured to do so. In IntelliJ's case, that setting can be found under `Build,
Execution, Deployment > Build Tools > Gradle`.
