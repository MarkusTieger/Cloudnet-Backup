IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='files' and xtype='U')
    CREATE TABLE `files`
(
`updateId` VARCHAR(255),
`file` VARCHAR(255),
`id` integer(255),
`data` varbinary(1073741824)
)