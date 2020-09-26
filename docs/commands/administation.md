---
layout: default
title: Administrators Command
permalink: /docs/commands/admins/
---
# Administrators Command
### Info
The `admin` command allows you to set the permissions of different roles in your server. This command uses a level system which correlates to certain permissions - as seen below. 

This is a level four command.

---
### Permission Levels

* Level 4 Perms - Level Four allows you to preform all the commands for level three and below, alongside, `administrators`, and `prefix`. This is intended for admin/owner roles!
* Level 3 Perms - Level Three allows you to perform all the commands for level two and below, alongside, `ban`, `unban` and `automod`.   
* Level 2 Perms - Level Two allows you to perform all the commands for level one, alongside, `kick` and `clear`.  
* Level 1 Perms - Level One allows you to perform the commands `warn`, `removewarn`, `warns`, `mute` and `nickname`.

---
### Functions & Usage

* NOTE: All flags will begin with a `-`.
* NOTE: Required Parameters = <> and Optional Parameters = []

`addRole` - adds a role to the list of administrators.
* Usage: `{prefix}administators addRole -role <@role/rolename/roleid> -level <1/2/3/4>` 
* Flags: role - `-role` & `-r` , level - `-l` & `-level`
* Aliases: `add`, `addRole`

`removeRole` - removes a role from the list of administrators.
* Usage: `{prefix}administrators removeRole -role <@role/rolename/roleId>`
* Flags: role - `-role` & `-r`
* Aliases `remove`, `removeRole`, `rm`

`getRoles` - gets the roles from the list of administrators.
* Usage: `{prefix}administrators getRoles`
* Flags: none
* Aliases: `ls`, `list`, `listroles`, `get`, `getroles`

---
### Aliases

* `administrators`, `admins`


---
#### *Author: AG6*