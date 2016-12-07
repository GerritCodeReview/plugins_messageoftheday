Build
=====

This plugin is built with Buck.
Clone or link this plugin to the plugins directory of Gerrit tree
and issue the command:

```
  buck build plugins/messageoftheday
```

The output is created in

```
  buck-out/gen/plugins/messageoftheday/messageoftheday.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```