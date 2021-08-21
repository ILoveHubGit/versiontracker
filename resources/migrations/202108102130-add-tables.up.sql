CREATE TABLE environments
(id INTEGER PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(30) UNIQUE,
 comment VARCHAR(255),
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);

CREATE TABLE nodes
(id  INTEGER PRIMARY KEY AUTO_INCREMENT,
 env_id INTEGER,
 name VARCHAR(30),
 type VARCHAR(30),
 version VARCHAR(30),
 deploymentdate TIMESTAMP,
 comment VARCHAR(255),
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);

CREATE UNIQUE INDEX IN_NODES ON nodes
(env_id, name, version);

CREATE TABLE subnodes
(id  INTEGER PRIMARY KEY AUTO_INCREMENT,
 nod_id INTEGER,
 name VARCHAR(30),
 version VARCHAR(30),
 deploymentdate TIMESTAMP,
 comment VARCHAR(255),
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);

CREATE UNIQUE INDEX IN_SUBNODES ON subnodes
(nod_id, name, version);

CREATE TABLE links
(id  INTEGER PRIMARY KEY AUTO_INCREMENT,
 env_id INTEGER,
 name VARCHAR(30),
 version VARCHAR(30),
 type VARCHAR(30),
 deploymentdate TIMESTAMP,
 comment VARCHAR(255),
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);

CREATE UNIQUE INDEX IN_LINKS ON links
(env_id, name, version);

CREATE TABLE sources
(id INTEGER PRIMARY KEY AUTO_INCREMENT,
 lin_id INTEGER,
 nod_id INTEGER DEFAULT -1,
 sub_id INTEGER DEFAULT -1,
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);


CREATE TABLE targets
(id INTEGER PRIMARY KEY AUTO_INCREMENT,
 lin_id INTEGER,
 nod_id INTEGER DEFAULT -1,
 sub_id INTEGER DEFAULT -1,
 timestamp TIMESTAMP(7) DEFAULT CURRENT_TIMESTAMP);
