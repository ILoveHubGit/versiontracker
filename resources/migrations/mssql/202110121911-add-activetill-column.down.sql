ALTER TABLE environments
DROP COLUMN activetill;
--;;
ALTER TABLE nodes
DROP COLUMN activetill;
--;;
ALTER TABLE subnodes
DROP COLUMN activetill;
--;;
ALTER TABLE links
DROP COLUMN activetill;
--;;
ALTER TABLE sources
DROP COLUMN activetill;
--;;
ALTER TABLE targets
DROP COLUMN activetill;
