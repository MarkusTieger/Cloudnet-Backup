IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='extra_files' and xtype='U')
    CREATE TABLE `extra_files`
(
`updateId` VARCHAR(255),
`name` VARCHAR(255),
`file` VARCHAR(255)
)