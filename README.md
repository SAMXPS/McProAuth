# McProAuth
McProAuth is a Minecraft Premium Account login detector for your Minecraft Network.
Minecraft Premium Players log in directly into your lobby/preferred server without any passwords.
Non-premium Players need to set up a password in order to have access to your server.

## Requirements

To use McProAuth, you need a BungeeCord server running version 1.8 or above as well as a Spigot (or Paper) server dedicated only for login purposes. 
i.e. The Spigot login server won't be used for anything but receiving your 'offline mode' players and asking them for log-in credentials. 

Configuration Notes: 
- Your BungeeCord proxy must be configured with ``online_mode: true`` and ``ip_forward: true`` in ``config.yml`` 
- Your Spigot server must be configured to hook with BungeeCord, it is, ``bungeecord: true`` in ``spigot.yml``

## Easy. Secure. Effective.

McProAuth uses blowfish Bcrypt to encrypt all users passwords and is compatible with PHP password_verify() function.
BCrypt blowfish is a one-way encryption technology, that is, once the password is encrypted there is no way to revert it back to plain text. 
Furthermore, the current implementation on this plugin use random salts while generating the cyphers.

## Website Integration

With McProAuth, it is easy for developers to build website login features using the same database as this plugin.

## UUID Conversion for OfflineMode Players

McProAuth automatically creates name-based UUIDs for your non-premium minecraft players.

