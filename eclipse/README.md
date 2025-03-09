# Manorrock Assistant Eclipse Plugin

## Setting up Dependencies

To seed the `lib` directory with all required dependencies, you can use the following Maven command:

```bash
mvn -f copy-dependencies.xml package
```

This will download all required dependencies and place them in the lib directory as configured in the copy-dependencies.xml file. Make sure to run this command from the root directory of the project.
