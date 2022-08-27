IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='backups' and xtype='U')
    CREATE TABLE `backups`
(
`updateId` VARCHAR(255),
`version` VARCHAR(255),
`time` TIMESTAMP on update CURRENT_TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)