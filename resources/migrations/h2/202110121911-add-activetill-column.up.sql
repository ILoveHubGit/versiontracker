ALTER TABLE environments
ADD COLUMN activetill TIMESTAMP;
--;;
ALTER TABLE nodes
ADD COLUMN activetill TIMESTAMP;
--;;
ALTER TABLE subnodes
ADD COLUMN activetill TIMESTAMP;
--;;
ALTER TABLE links
ADD COLUMN activetill TIMESTAMP;
--;;
ALTER TABLE sources
ADD COLUMN activetill TIMESTAMP;
--;;
ALTER TABLE targets
ADD COLUMN activetill TIMESTAMP;
