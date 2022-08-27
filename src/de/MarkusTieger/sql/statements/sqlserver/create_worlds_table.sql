IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='worlds' and xtype='U')
    CREATE TABLE `worlds`
(
`updateId` VARCHAR(255),
`name` VARCHAR(255),
`file` VARCHAR(255)
)