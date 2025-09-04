# FamilyHub (JavaFX + SQLite)

A desktop app (no server, no external DB) that bundles a simple personal/family hub:
Lists, Calendar, Recipes, Messages, Gallery, Contacts, Activity, and premium-like sections
Documents, Budget, Meals, Timetable, Map.

## Requirements

- JDK 21+ installed
- Maven 3.9+

## Run

```bash
mvn -q -DskipTests javafx:run
```

The SQLite DB is created under `~/.myfamilyhub/app.db` and migrations are automatically applied via Flyway.

## Package

You can build a runnable jar:

```bash
mvn -q package
java -jar target/family-hub-1.0.0.jar
```

(For native installers, use `jpackage` with your platform JDK.)
