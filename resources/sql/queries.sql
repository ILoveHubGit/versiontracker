-- Below queries exposed via the API

-- :name create-environment! :i! :raw
/* :doc Creates a new environment record
   Kreas novan median rekordon
   Params: {:name "name" :comment "comment"}
*/
INSERT INTO environments
(name, comment)
VALUES (:name, :comment);

-- :name get-environments :?
/* :doc Retrieves the list of environments
   Rekuperas la liston de medioj
   Params: none
*/
SELECT id, name, comment
FROM environments;

-- :name get-environment :? :1
/* :doc Retrieves a specific environment
   Rekuperas specifa medio
   Params: {:env_name "name"}
*/
SELECT name, comment
FROM environments
WHERE name = :env_name;

-- :name create-node! :i! :raw
/* :doc Creates a new node in an environment
   Kreas nuvon nodon en medio
   Params: {:env_id ID :name "name" :type "type" :version "version" :deploymentdate "date-time" :comment "comment"}
*/
INSERT INTO nodes
(env_id, name, type, version, deploymentdate, comment)
VALUES (:env_id, :name, :type, :version, :deploymentdate, :comment)

-- :name get-all-nodes :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT *
FROM nodes

-- :name get-nodes :?
/* :doc Retrieve the nodes as deployed before :date, or the last situation
   Rekuperu la nodojn kiel deplojitajn antaŭe :dato aŭ la lasta situacio
   Params: {:env_id id :date "date-time"} :date is optional - estas nedeviga
*/
SELECT name, type, version, deploymentdate AS depdate, comment
FROM nodes
WHERE id in (SELECT MAX(id)
              FROM nodes
              --~ (if (contains? params :date) "WHERE timestamp < :date" "WHERE timestamp < CURRENT_TIMESTAMP")
              GROUP BY name)
AND env_id = :env_id

-- :name get-node :? :1
/* :doc Retrieve a specific node from an environement
   Rekuperas specifa nodon de medio
   Params: {:env_id ID :nod_name "name" :nod_version "version"}
*/
SELECT name
FROM nodes
WHERE env_id = :env_id
AND name = :nod_name
AND version = :nod_version

-- :name create-subnode! :i! :raw
/* :doc Creates a new subnode for a node
   Kreas nuvon subnodon por nodon
   Params: {:nod-id ID, :name "name" :version "version" :deploymentdate "date-time" :comment "comment"}
*/
INSERT INTO subnodes
(nod_id, name, version, deploymentdate, comment)
VALUES :nod_id, :name, :version, :deploymentdate, :comment)

-- :name get-all-subnodes :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT *
FROM subnodes

-- :name get-subnodes :?
/* :doc Retrieve the subnodes for a specific node
   Rekuperu la subnodojn por specifa nodo
   Params: {:nod_name "name" :nod_version "version" :date "date-time"} :date is optional - estas nedeviga
*/
SELECT name, version, deploymentdate AS depdate, comment
FROM subnodes
WHERE id in (SELECT MAX(id)
               FROM subnodes
               --~ (if (contains? params :date) "WHERE timestamp < :date" "WHERE timestamp < CURRENT_TIMESTAMP")
               GROUP BY name)
AND nod_id = :nod_id

-- :name get-subnode :? :1
/* :doc Retrieve a specific subnode from a node in an environment
   Rekuperu specifan subnodon de nodo en medio
   Params: {:nod_id ID :sub_name "name" :sub_version "version"}
*/
SELECT name
FROM subnodes
WHERE nod_id = :nod_id
AND name = :sub_name
AND version = :sub_version


-- :name create-link! :i! :raw
/* :doc Creates a new link in the environment between existing (sub)nodes
   Kreas novan ligon en la medio inter ekzistantaj (sub)nodoj
   Params: {:env_id env_id :name "name" :type "type" :version "version" :deploymentdate "date-time" :comment "comment"}
*/
INSERT INTO links
(env_id, name, type, version, deploymentdate, comment)
VALUES (:env_id, :name, :type, :version, :deploymentdate, :comment)

-- :name get-all-links :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT *
FROM links

-- :name get-links :?
/* :doc Retrieve the links from an environment
   Rekuperas la ligo de medio
   Params: {:env_id ID :date "date-time"} :date is optional - estas nedeviga
*/
SELECT l.name, l.type, l.version, l.deploymentdate AS depdate, l.comment, FORMATDATETIME(l.timestamp, 'yyyy-MM-dd HH:mm:ss') AS insertdate,
       sn.name AS sourceName, sn.version AS sourceVersion, ssn.name AS sourceSubNode, ssn.version AS sourceSubVersion,
       tn.name AS targetName, tn.version AS targetVersion, tsn.name AS targetSubNode, tsn.version AS targetSubVersion
FROM links as l
LEFT OUTER JOIN sources AS s ON l.id = s.lin_id
                            AND s.nod_id IN (SELECT MAX(nod_id)
                                             FROM sources
                                             --~ (if (contains? params :date) "WHERE timestamp < :date" "WHERE timestamp < CURRENT_TIMESTAMP")
                                             AND lin_id = s.lin_id
                                             GROUP BY lin_id)
LEFT OUTER JOIN nodes AS sn ON s.nod_id = sn.id
LEFT OUTER JOIN subnodes AS ssn ON s.sub_id = ssn.id
LEFT OUTER JOIN targets AS t ON l.id = t.lin_id
                            AND t.nod_id IN (SELECT MAX(nod_id)
                                             FROM targets
                                             --~ (if (contains? params :date) "WHERE timestamp < :date" "WHERE timestamp < CURRENT_TIMESTAMP")
                                             AND lin_id = t.lin_id
                                             GROUP BY lin_id)
LEFT OUTER JOIN nodes AS tn ON t.nod_id = tn.id
LEFT OUTER JOIN subnodes AS tsn ON t.sub_id = tsn.id
WHERE l.id IN (SELECT MAX(id)
               FROM links
               --~ (if (contains? params :date) "WHERE timestamp < :date" "WHERE timestamp < CURRENT_TIMESTAMP")
               GROUP BY name)
AND l.env_id = :env_id

-- :name create-source! :i! :raw
/* :doc Adds a new version of a source to a link
   Aldonas novan version de fonto al ligilo
   Params: {:lin_id lin-id :nod-name "name" :nod_version "version" :sub_name "name" :sub_version "version"}
*/
INSERT INTO sources
(lin_id, nod_id, sub_id)
VALUES (:lin_id,
        ISNULL(SELECT id FROM nodes WHERE name = :nod_name AND version = :nod_version,-1),
        ISNULL(SELECT id FROM subnodes WHERE name = :sub_name AND version = :sub_version,-1))

-- :name get-all-sources :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT *
FROM sources

-- :name get-sources :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT lin_id, nod_id, sub_id
FROM sources
WHERE lin_id = :lin_id

-- :name create-target! :i! :raw
/* :doc Adds a new version of a target to a link
   Aldonas novan version de celo al ligilo
   Params: {:lin_id lin-id :nod-name "name" :nod_version "version" :sub_name "name" :sub_version "version"}
*/
INSERT INTO targets
(lin_id, nod_id, sub_id)
VALUES (:lin_id,
        ISNULL(SELECT id FROM nodes WHERE name = :nod_name AND version = :nod_version,-1),
        ISNULL(SELECT id FROM subnodes WHERE name = :sub_name AND version = :sub_version,-1))

-- :name get-all-targets :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT *
FROM targets

-- :name get-targets :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT lin_id, nod_id, sub_id
FROM targets
WHERE id = :lin_id)


-- Above queries exposed via the API


-- Below queries which will not be exposed via the API
-- :name show-tables :?
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SHOW TABLES;

-- :name get-environment-id :? :1
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT id
FROM environments
WHERE name = :env_name

-- :name get-maxId-nodes :? :1
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT MAX(id)
FROM nodes
GROUP BY name

-- :name get-node-id :? :1
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT id
FROM nodes
WHERE env_id = :env_id
AND name = :name
AND version = :version

-- :name get-subnode-id :? :1
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT id
FROM subnodes
WHERE nod_id = :nod_id
AND name = :name
AND version = :version

-- :name get-link-id :? :1
/* :doc For developers only
   Nur por programistoj
   Params: none
*/
SELECT id
FROM links
WHERE env_id = env_id
AND name = :name
AND version = :version
