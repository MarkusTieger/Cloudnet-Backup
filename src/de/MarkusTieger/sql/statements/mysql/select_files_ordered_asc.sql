SELECT *
FROM `files`
WHERE
`updateId` = ?
AND
`file` = ?
ORDER
BY `id`
ASC