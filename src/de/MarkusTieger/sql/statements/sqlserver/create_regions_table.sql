IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='regions' and xtype='U')
    CREATE TABLE `regions`
(
`updateId` VARCHAR(255),
`name` VARCHAR(255),
`filename` VARCHAR(255),
`file` VARCHAR(255)
)