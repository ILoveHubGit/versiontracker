SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[environments](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[name] [varchar](30) NULL,
	[comment] [varchar](255) NULL,
	[Timestamp] [datetime] NULL,
UNIQUE NONCLUSTERED
(
	[name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
--;;
SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[links](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[env_id] [int] NULL,
	[name] [varchar](30) NULL,
	[version] [varchar](30) NULL,
	[type] [varchar](30) NULL,
	[deploymentdate] [datetime] NULL,
	[comment] [varchar](255) NULL,
	[Timestamp] [datetime] NULL
) ON [PRIMARY]
--;;
CREATE UNIQUE CLUSTERED INDEX [Links_Name_Version] ON [dbo].[links]
(
	[env_id] ASC,
	[name] ASC,
	[version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
--;;
SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[nodes](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[env_id] [int] NULL,
	[name] [varchar](30) NULL,
	[type] [varchar](30) NULL,
	[version] [varchar](30) NULL,
	[deploymentdate] [datetime] NULL,
	[comment] [varchar](255) NULL,
	[Timestamp] [datetime] NULL
) ON [PRIMARY]
--;;
CREATE UNIQUE CLUSTERED INDEX [Nodes_Name_Version] ON [dbo].[nodes]
(
	[env_id] ASC,
	[name] ASC,
	[version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
--;;
SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[sources](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[lin_id] [int] NULL,
	[nod_id] [int] NULL,
	[sub_id] [int] NULL,
	[Timestamp] [datetime] NULL
) ON [PRIMARY]
--;;
SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[subnodes](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[nod_id] [int] NULL,
	[name] [varchar](30) NULL,
	[version] [varchar](30) NULL,
	[deploymentdate] [datetime] NULL,
	[comment] [varchar](255) NULL,
	[Timestamp] [datetime] NULL
) ON [PRIMARY]
--;;
CREATE UNIQUE CLUSTERED INDEX [Subnodes_Name_Version] ON [dbo].[subnodes]
(
	[nod_id] ASC,
	[name] ASC,
	[version] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
--;;
SET ANSI_NULLS ON
--;;
SET QUOTED_IDENTIFIER ON
--;;
CREATE TABLE [dbo].[targets](
	[id] [int] IDENTITY(1,1) NOT NULL,
	[lin_id] [int] NULL,
	[nod_id] [int] NULL,
	[sub_id] [int] NULL,
	[Timestamp] [datetime] NULL
) ON [PRIMARY]
--;;
ALTER TABLE [dbo].[environments] ADD CONSTRAINT [DF_environments_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
ALTER TABLE [dbo].[links] ADD CONSTRAINT [DF_links_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
ALTER TABLE [dbo].[nodes] ADD CONSTRAINT [DF_nodes_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
ALTER TABLE [dbo].[sources] ADD CONSTRAINT [DF_sources_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
ALTER TABLE [dbo].[subnodes] ADD CONSTRAINT [DF_subnodes_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
ALTER TABLE [dbo].[targets] ADD CONSTRAINT [DF_targets_Timestamp] DEFAULT (getdate()) FOR [Timestamp]
--;;
