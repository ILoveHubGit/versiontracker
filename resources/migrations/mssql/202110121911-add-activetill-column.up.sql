ALTER TABLE environments
ADD activetill [datetime];
--;;
ALTER TABLE nodes
ADD activetill [datetime];
--;;
ALTER TABLE subnodes
ADD activetill [datetime];
--;;
ALTER TABLE links
ADD activetill [datetime];
--;;
ALTER TABLE sources
ADD activetill [datetime];
--;;
ALTER TABLE targets
ADD activetill [datetime];
