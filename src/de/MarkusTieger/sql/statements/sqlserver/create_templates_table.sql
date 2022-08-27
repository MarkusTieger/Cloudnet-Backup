IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='templates' and xtype='U')
    CREATE TABLE `templates`
(
`updateId` VARCHAR(255),
`name` VARCHAR(255),
`file` VARCHAR(255)
)