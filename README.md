# Version Tracker  (Version 0.1.0-SNAPSHOT)
It looks like these days almost every IT company is using CI/CD and for sure they all do this for a good reason. But is it always clear what software versions are running in what environment? In (larger) companies with lots of in house developed software, maybe in combination with commercial software, it might be difficult to keep track of all the software versions deployed to their test, acceptance or even production environment. That's why I started creating this application; to visualize the architectural landscape of an IT-environment and integrate it with your CI/CD.

The idea is that within an environment (test, acceptance, production), applications (systems) can be added as nodes. These nodes can have sub-nodes (website on Apache server). Then both nodes and sub-nodes can be linked together through there interfaces as sources (sending) and targets (receiving).

Below a representation of the relation between the Sources, Interfaces and Targets
![alt text][screen-table]

The following API calls can be used to integrate with your CI/CD.
![alt text][screen-swagger]

## Prerequisites
To make a build yourself you will need [Leiningen][1] 2.0 or above and Java 1.8 or above installed.

## Running
Download this project and type:

    lein uberjar

to generate the versiontracker.jar file in the subfolder target/uberjar.
Create a file (config.edn) see dev-config.edn for an example. After this the application can be started with the following command:

    java -Dconf=config.edn -jar versiontracker.jar

If you run this application for the first time you need to run this command before you can start to create the necessary database.

    java -jar versiontracker.jar migrate

## Releases
### Version 0.2.0

- Added Graph View

### Version 0.1.0

- Initial version included table view of interfaces

## License
Copyright © 2021 ILoveHubGit

[1]: https://github.com/technomancy/leiningen

[screen-table]: /resources/public/img/versiontracker-0.1.0.png "Table view"
[screen-swagger]: /resources/public/img/vt-swagger-0.1.0.png "Swagger view"
