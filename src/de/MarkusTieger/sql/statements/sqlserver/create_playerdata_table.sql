IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='playerdata' and xtype='U')
    CREATE TABLE `playerdata`
(
`updateId` VARCHAR(255),
`name` VARCHAR(255),
`filename` VARCHAR(255),
`file` VARCHAR(255)
)