# Version Tracker
<img src="/resources/public/img/vt-logo.svg" width="50%" align="left"/>

Almost every IT company is more and more using GitLab and for sure they all do this for a good reason. However, when not yet all applications are controlled in GitLab it might not always be clear what software versions are running in what environment? In (larger) companies with lots of in house developed software, for sure in combination with commercial or open source software. It might be difficult to keep track of the software versions deployed to their test, acceptance or even production environment. That's why I started creating this application; to visualize the architectural landscape of an IT-environment and integrate it with your deployment scripts via API calls.

The idea is that within an environment (test, acceptance, production), applications (systems) can be added as nodes. These nodes can have sub-nodes (independent deployable functions). Then these nodes can, with or without their sub-nodes, be linked together through there interfaces as sources (sending) and/or targets (receiving).

Below a representation of the relation between the Sources, Interfaces and Targets
![Table View][screen-table]

The following API calls can be used to integrate with your deployment scripts.
![Swagger page][screen-swagger]

## Prerequisites
To make a build yourself you will need [Leiningen][1] 2.0 or above and Java 1.8 or above installed.

## Running
Download this project and type:

    lein uberjar

to generate the versiontracker.jar file in the subfolder target/uberjar.
Create a file (config.edn) see dev-config.edn for an example. After this the application can be started with the following command:

    java -Dconf=config.edn -jar versiontracker.jar

If you run this application for the first time you need to run this command before you can start to create the necessary database tables.

    java -Dconf=config.edn -jar versiontracker.jar migrate

Currently two databases are supported:
 - H2 (database is created if it does not exist)
 - MS-SQL (Database need to be created before running migration commando)

## Releases
### Version 0.2.0

- Bug fixes
- Export to PDF added
- New icon for Links (stream, queue, real-time, realtime)
- Extra information in Swagger page

### Version 0.1.6

- Bug Fixes

### Version 0.1.5

- Bug fixes
- Added tests

### Version 0.1.4

- Added optional query parameter "keepVersions" also to Add SubNodes
- Added a logo

### Version 0.1.3

- Changed get-links query to be able to show links with multiple nodes
- Added an optional query parameter "keepVersions" to Add Nodes and Add Links
  - Possible values are:
    - None - This will inactivate all existing items
    - Last - This will inactivate all existing items except the last one
    - AllButOldest - This will keep all existing items active except the oldest one
    - All - This will keep all existing items active
  - If this parameter is used the new item will be linked the all node(s) or link that needs to stay active

### Version 0.1.2

- Prepared Graph View
- Added support for MS-SQL database

### Version 0.1.0

- Initial version included table view of interfaces

## License
Copyright © 2021 ILoveHubGit

[1]: https://github.com/technomancy/leiningen

[screen-table]: /resources/public/img/versiontracker-0.2.0.png "Table view"
[screen-swagger]: /resources/public/img/vt-swagger-0.2.0.png "Swagger view"
[VT-image]: /resources/public/img/vt-logo.svg "Logo"
