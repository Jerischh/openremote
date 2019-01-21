# OpenRemote v3

[Source](https://github.com/openremote/openremote) **·** [Documentation](https://github.com/openremote/openremote/wiki) **·** [Community](https://groups.google.com/forum/#!forum/openremotecommunity) **·** [Issues](https://github.com/openremote/openremote/issues) **·** [Docker Images](https://hub.docker.com/u/openremote/) **·** [OpenRemote Inc.](https://openremote.io)

We are currently working on v3 of the OpenRemote platform. This is **beta** software that should be used only for development.

If you want to try OpenRemote v2, [read the OpenRemote v2 documentation](https://github.com/openremote/Documentation/wiki).

## Quickstart

Checkout this project or at a minimum, get the Docker Compose [profiles](profile/) and ensure you have [Docker Community Edition](https://www.docker.com/) installed, then either use images from docker hub (easiest) or build the images locally first.

### Use images from Docker Hub
We publish our docker images to [Docker Hub](https://hub.docker.com/u/openremote/), please be aware that the published images may be out of date compared to the codebase, if you want to run the latest code then you may need to build the images first. To run a demo using Docker Hub images simply run the following command from the repo root (or directory containing the `profile` directory if you only downloaded the profiles):

```
docker-compose -p openremote -f profile/demo.yml up --no-build
```

### Build images locally
Alternatively you can build the docker images locally, to do this please refer to the [Developer Guide](https://github.com/openremote/openremote/wiki/Developer-Guide%3A-Building-the-code) to build the code, once the code is built

```
docker-compose -p openremote -f profile/demo.yml up --build
```

Once you have running docker containers you can Access the manager UI and API on https://localhost/ with username `admin` and password `secret`. Accept the 'insecure' self-signed SSL certificate.
                                                
The console app of `customerA` can be accessed on https://localhost/customerA/ with username `testuser3` and password `testuser3`.

Stop the stack and remove all unused data volumes (alternatively delete only the openremote volumes if you don't want all unused volumes to be removed) with:

```
docker-compose -p openremote -f profile/demo.yml down
docker volume prune
```

To preserve data between restarts, don't delete the Docker volumes `openremote_deployment-data` and `openremote_postgresql-data` between restarts. You must also change `SETUP_WIPE_CLEAN_INSTALL` to `false` in `demo.yml`!

More configuration options of the images are documented [in the deploy.yml profile](https://github.com/openremote/openremote/blob/master/profile/deploy.yml).


## Contributing to OpenRemote

We work with Java, Groovy, JavaScript, Gradle, Docker, and a wide range of APIs and protocol implementations. Clone or checkout this project and send us pull requests, ensure that code is covered by tests and that the full test suite passes.

For more information and how to set up a development environment, see the [Developer Guide](https://github.com/openremote/openremote/wiki).


## Discuss OpenRemote

Join us on the [community group](https://groups.google.com/forum/#!forum/openremotecommunity).
