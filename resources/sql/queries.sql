-- :name create-environment! :! :n
-- :doc creates a new environment record
INSERT INTO environments
(name, comment)
VALUES (:name, :comment);

-- :name get-environments :?
-- :doc Retrieves the list of environments
SELECT id, name, comment
FROM environments;

-- :name get-environment :?
-- :doc Retrieves a specific of environment
SELECT name, comment
FROM environments
WHERE name = :env_name;

-- :name delete-environment! :! :n
-- :doc deletes a user record given the id
DELETE FROM environments
WHERE id = :id;

-- :name create-node! :! :n
-- :doc Creates a new node in the environment
INSERT INTO nodes
(env_id, name, type, version, deploymentdate, comment)
VALUES ((SELECT id FROM environments WHERE name = :env_name), :name, :type, :version, :deploymentdate, :comment)

-- :name get-all-nodes :?
-- :doc Used to be able to test the get-nodes query
SELECT *
FROM nodes

-- :name get-nodes :?
-- :doc Retrieve the nodes as deployed before :date, or the last situation
SELECT name, type, version, deploymentdate AS depdate, comment
FROM nodes
WHERE id in (SELECT MAX(id)
              FROM nodes
              --~ (when (contains? params :date) "WHERE timestamp < :date")
              GROUP BY name)
AND env_id = (SELECT id FROM environments WHERE name = :env_name)

-- :name get-node :?
-- :doc Retrieve a specific node from an environement
SELECT name
FROM nodes
WHERE env_id = (SELECT id FROM environments WHERE name = :env_name)
AND name = :nod_name
AND version = :nod_version

-- :name create-subnode! :! :n
-- :doc Creates a new subnode for a node
INSERT INTO subnodes
(nod_id, name, version, deploymentdate, comment)
VALUES ((SELECT id
          FROM nodes
          WHERE name = :nod_name
          AND version = :nod_version
          AND env_id = (SELECT id FROM environments WHERE name = :env_name)),
        :name, :version, :deploymentdate, :comment)

-- :name get-all-subnodes :?
-- :doc Used to be able to test the get-subnodes query
SELECT *
FROM subnodes

-- :name get-subnodes :?
SELECT name, version, deploymentdate AS depdate, comment
FROM subnodes
WHERE id in (SELECT MAX(id)
               FROM subnodes
               --~ (when (contains? params :date) "WHERE timestamp < :date")
               GROUP BY name)
AND nod_id = (SELECT id FROM nodes WHERE name = :nod_name AND version = :nod_version)

-- :name get-subnode :?
-- :doc Retrieve a specific subnode from an environement
SELECT name
FROM subnodes
WHERE nod_id = (SELECT id
                FROM nodes
                WHERE env_id = (SELECT id FROM environments WHERE name = :env_name)
                AND name = :nod_name
                AND version = :nod_version)
AND name = :sub_name
AND version = :sub_version


-- :name create-link! :! :n
-- :doc Creates a new link in the nevironment between existing (sub)nodes
INSERT INTO links
(env_id, name, type, version, deploymentdate, comment)
VALUES ((SELECT id FROM environments WHERE name = :env_name),
        :name, :type, :version, :deploymentdate, :comment)

-- :name get-all-links :?
-- :doc Used to be able to test the get-links query
SELECT *
FROM links

-- :name get-links :?
-- :doc "Retrieve the links from an enviroment"
SELECT l.name, l.type, l.version, l.deploymentdate AS depdate, l.comment, FORMATDATETIME(l.timestamp, 'yyyy-MM-dd HH:mm:ss') AS insertdate,
       sn.name AS sourceName, sn.version AS sourceVersion, ssn.name AS sourceSubNode, ssn.version AS sourceSubVersion,
       tn.name AS targetName, tn.version AS targetVersion, tsn.name AS targetSubNode, tsn.version AS targetSubVersion
FROM links as l
LEFT OUTER JOIN sources AS s ON l.id = s.lin_id
LEFT OUTER JOIN nodes AS sn ON s.nod_id = sn.id
LEFT OUTER JOIN subnodes AS ssn ON s.sub_id = ssn.id
LEFT OUTER JOIN targets AS t ON l.id = t.lin_id
LEFT OUTER JOIN nodes AS tn ON t.nod_id = tn.id
LEFT OUTER JOIN subnodes AS tsn ON t.sub_id = tsn.id
WHERE l.id in (SELECT MAX(id)
               FROM links
               --~ (when (contains? params :date) "WHERE timestamp < :date")
               GROUP BY name)
AND l.env_id = (SELECT id FROM environments WHERE name = :env_name)
AND (s.nod_id <> -1 OR t.nod_id <> -1)

-- :name create-source! :! :n
-- :doc Adds a new version of a source to a link
INSERT INTO sources
(lin_id, nod_id, sub_id)
VALUES (:lin_id,
        ISNULL(SELECT id FROM nodes WHERE name = :nod_name AND version = :nod_version,-1),
        ISNULL(SELECT id FROM subnodes WHERE name = :sub_name AND version = :sub_version,-1))

-- :name get-all-sources :?
-- :doc Used to be able to test the get-sources query
SELECT *
FROM sources

-- :name get-sources :?
-- :doc "Retrieve source id's"
SELECT lin_id, nod_id, sub_id
FROM sources
WHERE lin_id = :lin_id

-- :name create-target! :! :n
-- :doc Adds a new version of a target to a link
INSERT INTO targets
(lin_id, nod_id, sub_id)
VALUES (:lin_id,
        ISNULL(SELECT id FROM nodes WHERE name = :nod_name AND version = :nod_version,-1),
        ISNULL(SELECT id FROM subnodes WHERE name = :sub_name AND version = :sub_version,-1))

-- :name get-all-targets :?
-- :doc Used to be able to test the get-sources query
SELECT *
FROM targets

-- :name get-targets :?
-- :doc "Retrieve target id's"
SELECT lin_id, nod_id, sub_id
FROM targets
WHERE id = :lin_id)


-- Queries which will not be exposed via the API
-- :name show-tables :?
-- :doc Show all the tables
SHOW TABLES;

-- :name get-environment-id :? :1
-- :doc Gets the ID for a certain environment
SELECT id
FROM environments
WHERE name = :env_name

-- :name get-maxId-nodes :? :1
-- :doc Gets the IDs for the nodes
SELECT MAX(id)
FROM nodes
GROUP BY name

-- :name get-node-id :? :1
-- :doc Retrieve the id of a specific node
SELECT id
FROM nodes
WHERE name = :nod_name
AND version = :nod_version

-- :name get-subnode-id :? :1
-- :doc Retrieve the id of a specific node
SELECT id
FROM subnodes
WHERE name = :sub_name
AND version = :sub_version

-- :name get-link-id :? :1
-- :doc Retrieve the id of a specific link
SELECT id
FROM links
WHERE env_id = (SELECT id FROM environments WHERE name = :env_name)
AND name = :name
AND version = :version
